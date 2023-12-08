use std::sync::Arc;

use mio::net::{TcpListener, TcpStream};

use log::{debug, error};
use rustls::server::NoClientAuth;
use std::collections::HashMap;
use std::io;
use std::io::{Read, Write};
use std::net;

use rustls::{self, Certificate, PrivateKey};

/// This binds together a TCP listening socket, some outstanding
/// connections, and a TLS server configuration.
pub struct TlsServer {
    server: TcpListener,
    connections: HashMap<mio::Token, OpenConnection>,
    next_id: usize,
    tls_config: Arc<rustls::ServerConfig>,
    forward: u16,
}

// Token for our listening socket.
const LISTENER: mio::Token = mio::Token(0);

impl TlsServer {
    pub fn make_config(privkey: PrivateKey, certs: Vec<Certificate>) -> Arc<rustls::ServerConfig> {
        let client_auth = NoClientAuth::boxed();

        let suites = rustls::ALL_CIPHER_SUITES.to_vec();

        let versions = &[&rustls::version::TLS13].to_vec();

        let ocsp = Vec::new();

        let mut config = rustls::ServerConfig::builder()
            .with_cipher_suites(&suites)
            .with_safe_default_kx_groups()
            .with_protocol_versions(&versions)
            .expect("inconsistent cipher-suites/versions specified")
            .with_client_cert_verifier(client_auth)
            .with_single_cert_with_ocsp_and_sct(certs, privkey, ocsp, vec![])
            .expect("bad certificates/private key");

        config.key_log = Arc::new(rustls::KeyLogFile::new());

        config.session_storage = rustls::server::ServerSessionMemoryCache::new(256);

        config.ticketer = rustls::Ticketer::new().unwrap();

        Arc::new(config)
    }
    pub fn new(server: TcpListener, forward: u16, cfg: Arc<rustls::ServerConfig>) -> Self {
        Self {
            server,
            connections: HashMap::new(),
            next_id: 2,
            tls_config: cfg,
            forward: forward,
        }
    }

    pub fn accept(&mut self, registry: &mio::Registry) -> Result<(), io::Error> {
        loop {
            match self.server.accept() {
                Ok((socket, addr)) => {
                    debug!("Accepting new connection from {:?}", addr);

                    let tls_conn =
                        rustls::ServerConnection::new(Arc::clone(&self.tls_config)).unwrap();

                    let token = mio::Token(self.next_id);
                    self.next_id += 1;

                    let mut connection = OpenConnection::new(socket, token, self.forward, tls_conn);
                    connection.register(registry);
                    self.connections.insert(token, connection);
                }
                Err(ref err) if err.kind() == io::ErrorKind::WouldBlock => return Ok(()),
                Err(err) => {
                    println!(
                        "encountered error while accepting connection; err={:?}",
                        err
                    );
                    return Err(err);
                }
            }
        }
    }

    pub fn conn_event(&mut self, registry: &mio::Registry, event: &mio::event::Event) {
        let token = event.token();

        if self.connections.contains_key(&token) {
            self.connections
                .get_mut(&token)
                .unwrap()
                .ready(registry, event);

            if self.connections[&token].is_closed() {
                self.connections.remove(&token);
            }
        }
    }

    /**
     * tls转发器
     *
     * 监听 port 端口来接受 tls 协议
     * 将解码后的数据转发到 forward 这个明文端口上
     *
     * 从而可以为一些没有tls能力的服务提供tls能力
     */
    pub fn forward(port: u16, forward: u16, privkey: PrivateKey, certs: Vec<Certificate>) {
        let addr: net::SocketAddr = format!("0.0.0.0:{}", port).parse().unwrap();

        let config = TlsServer::make_config(privkey, certs);

        let mut listener = TcpListener::bind(addr).expect("cannot listen on port");
        let mut poll = mio::Poll::new().unwrap();
        poll.registry()
            .register(&mut listener, LISTENER, mio::Interest::READABLE)
            .unwrap();

        let mut tlsserv = TlsServer::new(listener, forward, config);

        let mut events = mio::Events::with_capacity(256);
        loop {
            poll.poll(&mut events, None).unwrap();

            for event in events.iter() {
                match event.token() {
                    LISTENER => {
                        tlsserv
                            .accept(poll.registry())
                            .expect("error accepting socket");
                    }
                    _ => tlsserv.conn_event(poll.registry(), event),
                }
            }
        }
    }
}

/// This is a connection which has been accepted by the server,
/// and is currently being served.
///
/// It has a TCP-level stream, a TLS-level connection state, and some
/// other state/metadata.
pub struct OpenConnection {
    socket: TcpStream,
    token: mio::Token,
    closing: bool,
    closed: bool,
    tls_conn: rustls::ServerConnection,
    back: TcpStream,
}

/// Open a plaintext TCP-level connection for forwarded connections.
fn open_back(forward: u16) -> TcpStream {
    let addr = net::SocketAddrV4::new(net::Ipv4Addr::new(127, 0, 0, 1), forward);
    let conn = TcpStream::connect(net::SocketAddr::V4(addr)).unwrap();
    conn
}

/// This used to be conveniently exposed by mio: map EWOULDBLOCK
/// errors to something less-errory.
fn try_read(r: io::Result<usize>) -> io::Result<Option<usize>> {
    match r {
        Ok(len) => Ok(Some(len)),
        Err(e) => {
            if e.kind() == io::ErrorKind::WouldBlock {
                Ok(None)
            } else {
                Err(e)
            }
        }
    }
}

impl OpenConnection {
    fn new(
        socket: TcpStream,
        token: mio::Token,
        forward: u16,
        tls_conn: rustls::ServerConnection,
    ) -> Self {
        let back = open_back(forward);
        Self {
            socket,
            token,
            closing: false,
            closed: false,
            tls_conn,
            back,
        }
    }

    /// We're a connection, and we have something to do.
    fn ready(&mut self, registry: &mio::Registry, ev: &mio::event::Event) {
        // If we're readable: read some TLS.  Then
        // see if that yielded new plaintext.  Then
        // see if the backend is readable too.
        if ev.is_readable() {
            self.do_tls_read();
            self.try_plain_read();
            self.try_back_read();
        }

        if ev.is_writable() {
            self.do_tls_write_and_handle_error();
        }

        if self.closing {
            let _ = self.socket.shutdown(net::Shutdown::Both);
            self.close_back();
            self.closed = true;
            self.deregister(registry);
        } else {
            self.reregister(registry);
        }
    }

    /// Close the backend connection for forwarded sessions.
    fn close_back(&mut self) {
        let back = &self.back;
        back.shutdown(net::Shutdown::Both).unwrap();
    }

    fn do_tls_read(&mut self) {
        // Read some TLS data.
        match self.tls_conn.read_tls(&mut self.socket) {
            Err(err) => {
                if let io::ErrorKind::WouldBlock = err.kind() {
                    return;
                }

                error!("read error {:?}", err);
                self.closing = true;
                return;
            }
            Ok(0) => {
                debug!("eof");
                self.closing = true;
                return;
            }
            Ok(_) => {}
        };

        // Process newly-received TLS messages.
        if let Err(err) = self.tls_conn.process_new_packets() {
            error!("cannot process packet: {:?}", err);

            // last gasp write to send any alerts
            self.do_tls_write_and_handle_error();

            self.closing = true;
        }
    }

    fn try_plain_read(&mut self) {
        // Read and process all available plaintext.
        if let Ok(io_state) = self.tls_conn.process_new_packets() {
            if io_state.plaintext_bytes_to_read() > 0 {
                let mut buf = vec![0u8; io_state.plaintext_bytes_to_read()];

                self.tls_conn.reader().read_exact(&mut buf).unwrap();

                debug!("plaintext read {:?}", buf.len());
                self.incoming_plaintext(&buf);
            }
        }
    }

    fn try_back_read(&mut self) {
        // Try a non-blocking read.
        let mut buf = [0u8; 1024];
        let mut back = &self.back;
        let rc = try_read(back.read(&mut buf));

        if rc.is_err() {
            error!("backend read failed: {:?}", rc);
            self.closing = true;
            return;
        }

        let maybe_len = rc.unwrap();

        // If we have a successful but empty read, that's an EOF.
        // Otherwise, we shove the data into the TLS session.
        match maybe_len {
            Some(0) => {
                debug!("back eof");
                self.closing = true;
            }
            Some(len) => {
                self.tls_conn.writer().write_all(&buf[..len]).unwrap();
            }
            None => {}
        };
    }

    /// Process some amount of received plaintext.
    fn incoming_plaintext(&mut self, buf: &[u8]) {
        self.back.write_all(buf).unwrap();
    }

    fn tls_write(&mut self) -> io::Result<usize> {
        self.tls_conn.write_tls(&mut self.socket)
    }

    fn do_tls_write_and_handle_error(&mut self) {
        let rc = self.tls_write();
        if rc.is_err() {
            error!("write failed {:?}", rc);
            self.closing = true;
        }
    }

    fn register(&mut self, registry: &mio::Registry) {
        let event_set = self.event_set();
        registry
            .register(&mut self.socket, self.token, event_set)
            .unwrap();

        registry
            .register(&mut self.back, self.token, mio::Interest::READABLE)
            .unwrap();
    }

    fn reregister(&mut self, registry: &mio::Registry) {
        let event_set = self.event_set();
        registry
            .reregister(&mut self.socket, self.token, event_set)
            .unwrap();
    }

    fn deregister(&mut self, registry: &mio::Registry) {
        registry.deregister(&mut self.socket).unwrap();

        registry.deregister(&mut self.back).unwrap();
    }

    /// What IO events we're currently waiting for,
    /// based on wants_read/wants_write.
    fn event_set(&self) -> mio::Interest {
        let rd = self.tls_conn.wants_read();
        let wr = self.tls_conn.wants_write();

        if rd && wr {
            mio::Interest::READABLE | mio::Interest::WRITABLE
        } else if wr {
            mio::Interest::WRITABLE
        } else {
            mio::Interest::READABLE
        }
    }

    fn is_closed(&self) -> bool {
        self.closed
    }
}
