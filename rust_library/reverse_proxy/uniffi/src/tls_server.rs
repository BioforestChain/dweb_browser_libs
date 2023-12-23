use std::sync::Arc;
use std::time::Duration;

use futures_util::Future;
use mio::net::{TcpListener, TcpStream};

use log::{debug, error};
use rustls::server::NoClientAuth;
use std::collections::HashMap;
use std::io::{self, ErrorKind};
use std::io::{Read, Write};
use std::{net, vec};

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

const TIMER: mio::Token = mio::Token(1);

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

        config.alpn_protocols = ["http/1.1".into()].to_vec();

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
                    info!("Accepting new connection from {:?}", addr);

                    let tls_conn =
                        rustls::ServerConnection::new(Arc::clone(&self.tls_config)).unwrap();

                    let token = mio::Token(self.next_id);
                    self.next_id += 1;

                    let mut connection = OpenConnection::new(socket, token, self.forward, tls_conn);
                    connection.register(registry);
                    self.connections.insert(token, connection);
                    info!("insert token: {}", token.0);
                }
                Err(ref err) if err.kind() == io::ErrorKind::WouldBlock => {
                    return Ok(());
                }
                Err(err) => {
                    return Err(err);
                }
            }
        }
    }

    pub fn conn_event(
        &mut self,
        registry: &mio::Registry,
        token: mio::Token,
        is_readable: bool,
        is_writable: bool,
    ) {
        // let token = event.token();
        if self.connections.contains_key(&token) {
            self.connections
                .get_mut(&token)
                .unwrap()
                .ready(registry, is_readable, is_writable);

            if self.connections[&token].is_closed() {
                info!("remove token: {}", token.0);
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
    pub async fn forward<F>(
        port: u16,
        forward: u16,
        privkey: PrivateKey,
        certs: Vec<Certificate>,
        on_listen: F,
    ) where
        F: Future + Send + 'static,
    {
        let addr: net::SocketAddr = format!("0.0.0.0:{}", port).parse().unwrap();
        info!("reverse_proxy forward {addr}");

        let config = TlsServer::make_config(privkey, certs);

        let mut listener = TcpListener::bind(addr).expect("cannot listen on port");
        let mut poll = mio::Poll::new().unwrap();
        poll.registry()
            .register(
                &mut listener,
                LISTENER,
                mio::Interest::READABLE | mio::Interest::WRITABLE,
            )
            .unwrap();

        let mut tlsserv = TlsServer::new(listener, forward, config);
        on_listen.await;

        let mut events = mio::Events::with_capacity(512);
        loop {
            if let Err(err) = poll.poll(&mut events, None) {
                //Some(Duration::from_millis(50))
                if interrupted(&err) {
                    continue;
                }
                break;
            }
            // 定时器触发，如果需要开启这个，说明是一些不得已的行为，请尽快修复
            // if events.is_empty() {
            //     let conn_events = tlsserv
            //         .borrow_mut()
            //         .connections
            //         .values()
            //         .filter(|conn| !conn.back_writting_buf.is_empty())
            //         .map(|conn| (conn.token, conn.event_set_for_socket()))
            //         .collect::<Vec<_>>();
            //     for (token, conn_event) in conn_events {
            //         tlsserv.conn_event(
            //             poll.registry(),
            //             token,
            //             conn_event.is_readable(),
            //             conn_event.is_writable(),
            //         );
            //     }
            //     continue;
            // }
            for event in events.iter() {
                match event.token() {
                    TIMER => {}
                    LISTENER => {
                        tlsserv
                            .accept(poll.registry())
                            .expect("error accepting socket");
                    }
                    _ => tlsserv.conn_event(
                        poll.registry(),
                        event.token(),
                        event.is_readable(),
                        event.is_writable(),
                    ),
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
    /** tls 来源的流 */
    socket: TcpStream,
    token: mio::Token,
    closing: bool,
    closed: bool,
    tls_conn: rustls::ServerConnection,
    /** 转发的目标 */
    back: TcpStream,
    back_writting_buf: Vec<u8>,
    back_wants_be_read: bool,
    back_wants_be_write: bool,
}

/// Open a plaintext TCP-level connection for forwarded connections.
fn open_back(forward: u16) -> TcpStream {
    let addr = net::SocketAddrV4::new(net::Ipv4Addr::new(127, 0, 0, 1), forward);
    let conn = TcpStream::connect(net::SocketAddr::V4(addr)).unwrap();
    conn
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
            back_writting_buf: vec![],
            back_wants_be_read: true,
            back_wants_be_write: true,
        }
    }

    /// We're a connection, and we have something to do.
    fn ready(&mut self, registry: &mio::Registry, is_readable: bool, is_writable: bool) {
        // 如果 tls_conn 可读 或者 back 还有需要进行写入的，那么几句将尝试读取 tls，并且执行 back 写入
        debug!("self.tls_conn.wants_read()={}", self.tls_conn.wants_read());
        // 这个函数是安全的，不需要判断条件就可以直接调用。如果可以它会尝试读取 tls_conn ，不会有风险，如果缓冲区存在数据它也会尝试向 back 写入数据，如果 back 的写入阻塞了，也做了处理，不会有问题
        self.try_read_tls_to_write_back();

        // 如果 tls_conn 可以进行写入就写入
        // 这里必须作出判断 wants_write = false 在来做这个 write_tls，因为 tls 的写入缓冲区不是我们管理，如果写入溢出了就糟糕了，所以这里先做 tls_write，然后在做 write_tls
        if self.tls_conn.wants_write() {
            self.do_tls_write_and_handle_error();
        }
        // 如果 tls_conn 不能对 socket 做写入，说明缓冲区区是空的，那么就将 back 的内容读出来给 tls_conn
        else {
            self.try_read_back_to_write_tls();
            // 完成对 tls_conn 的写入后，立刻尝试将 tls 的内容写入到 socket 中
            if self.tls_conn.wants_write() {
                self.do_tls_write_and_handle_error();
            }
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
        fn error(err: std::io::Error) -> () {
            warn!("close back fail:{}, but ignore this error", err);
            ()
        }
        back.shutdown(net::Shutdown::Both).unwrap_or_else(error);
    }

    fn try_read_tls_to_write_back(&mut self) {
        // 如果缓冲区中的数据不少，那么就跳过读取，直接进行写入
        let buf_for_write = if self.back_writting_buf.len() < 4096 {
            // 首先从 socket 中尝试读取数据到 tls_conn 中
            // TIP: 这里执行 read_tls 后必须执行 process_new_packets，然后也必须执行 reader
            match self.tls_conn.read_tls(&mut self.socket) {
                Err(err) => {
                    // 如果读取的异常 不是“阻塞”的错误，也不是“非致命”的错误，那么将会进度关闭
                    let error_kind = err.kind();
                    if error_kind != ErrorKind::WouldBlock && error_kind != ErrorKind::Interrupted {
                        error!("read_tls error {:?}", err);
                        self.closing = true;
                    }
                }
                Ok(size) => {
                    if size == 0 {
                        // eof，收到back关闭的信号了
                        self.closing = true;
                    }
                }
            };

            // 尝试将 tls_conn 中的数据解码出明文
            let try_buf = match self.tls_conn.process_new_packets() {
                Ok(io_state) => {
                    let io_size = io_state.plaintext_bytes_to_read();
                    if io_size > 0 {
                        let mut buf = vec![0u8; io_size];

                        self.tls_conn.reader().read_exact(&mut buf).unwrap();

                        Some(buf)
                    } else {
                        None
                    }
                }
                Err(err) => {
                    error!("cannot process packet: {:?}", err);

                    // 最后一次喘息，发送警报
                    self.do_tls_write_and_handle_error();

                    self.closing = true;
                    None
                }
            };

            // 组合之前遗留下来的数据
            let buf_for_write = match try_buf {
                None => self.back_writting_buf.to_vec(),
                Some(buf) => {
                    if self.back_writting_buf.is_empty() {
                        buf
                    } else {
                        [self.back_writting_buf.to_owned(), buf].concat()
                    }
                }
            };
            buf_for_write
        } else {
            self.back_writting_buf.to_owned()
        };

        // 将要向 back 写入数据，所以这里提前改变 back_wants_be_write 这个属性
        self.back_wants_be_write = false;
        // 将数据写入到 back 中
        if let false = buf_for_write.is_empty() {
            match tcpstream_write_buf(&self.back, &buf_for_write) {
                Err(err) => {
                    error!("tcpstream_write_buf error: {}", err);
                    self.closing = true;
                }
                Ok(len) => {
                    self.back_writting_buf = buf_for_write[len..].to_vec();
                    if let false = self.back_writting_buf.is_empty() {
                        debug!("back can go on to write {}", self.back_writting_buf.len());
                        self.back_wants_be_write = true
                    }
                }
            }
        }
    }

    fn try_read_back_to_write_tls(&mut self) {
        // 我们使用 16kb 来作为缓冲区
        let mut cache_buf = [0u8; 16384];
        // 将要从 back 读取数据，所以这里提前改变 back_wants_be_read 这个属性
        self.back_wants_be_read = false;
        // 从 back 中读取出一些数据
        match self.back.read(&mut cache_buf) {
            Ok(len) => {
                if len == 0 {
                    // eof，收到back关闭的信号了
                    self.closing = true;
                } else {
                    if len == cache_buf.len() {
                        self.back_wants_be_read = true
                    }
                    // 将数据写入到 tls_conn 中，让它做加密运算
                    let buf_for_write = &cache_buf[..len];
                    let wants_write = self.tls_conn.wants_write();
                    match self.tls_conn.writer().write_all(&buf_for_write) {
                        Err(err) => {
                            error!(
                                "tls_conn write_all error: wants_write={}, err={}",
                                wants_write, err
                            );
                            self.closing = true
                        }
                        Ok(()) => {}
                    }
                }
            }
            Err(ref err) => {
                // 如果读取的异常 不是“阻塞”的错误，也不是“非致命”的错误，那么将会进度关闭
                let error_kind = err.kind();
                if error_kind != ErrorKind::WouldBlock && error_kind != ErrorKind::Interrupted {
                    error!("back.read error {:?}", err);
                    self.closing = true;
                }
            }
        };
    }

    /// 通常是 tls_conn.wants_write() == true，所以才有调用这个函数
    fn do_tls_write_and_handle_error(&mut self) {
        // 这里返回的大小不用管，后面会调用 wants_write 即可判断 tls_conn 是否还能继续写入 socket
        match self.tls_conn.write_tls(&mut self.socket) {
            Ok(_) => {}
            Err(err) => {
                let error_kind = err.kind();
                if error_kind != ErrorKind::WouldBlock && error_kind != ErrorKind::Interrupted {
                    error!("tls_conn.write_tls to socket failed {:?}", err);
                    self.closing = true;
                }
            }
        }
    }

    /// 初始化注册，需要对 socket 和 back 都进行注册，其中 socket 跟随 tls_conn 的状态走， back 则是全注册
    fn register(&mut self, registry: &mio::Registry) {
        let socket_events = self.event_set_for_socket();
        registry
            .register(&mut self.socket, self.token, socket_events)
            .unwrap();

        registry
            .register(
                &mut self.back,
                self.token,
                mio::Interest::READABLE | mio::Interest::WRITABLE,
            )
            .unwrap();
    }

    // 在 reregister 中，只需要对 socket 进行重新注册，让它跟随 tls_conn 走，可以节省性能
    fn reregister(&mut self, registry: &mio::Registry) {
        let socket_events = self.event_set_for_socket();
        registry
            .reregister(&mut self.socket, self.token, socket_events)
            .unwrap();

        if self.back_wants_be_read || self.back_wants_be_write {
            registry
                .reregister(
                    &mut self.back,
                    self.token,
                    mio::Interest::READABLE | mio::Interest::WRITABLE,
                )
                .unwrap();
        }
    }

    fn deregister(&mut self, registry: &mio::Registry) {
        registry.deregister(&mut self.socket).unwrap();

        registry.deregister(&mut self.back).unwrap();
    }

    /// What IO events we're currently waiting for,
    /// based on wants_read/wants_write.
    fn event_set_for_socket(&self) -> mio::Interest {
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

fn interrupted(err: &io::Error) -> bool {
    err.kind() == io::ErrorKind::Interrupted
}

fn tcpstream_write_buf(mut socket: &TcpStream, mut buf: &[u8]) -> Result<usize, io::Error> {
    let mut size = 0;
    let mut has_interrupted_retry = false;
    while !buf.is_empty() {
        match socket.write(buf) {
            Ok(0) => {
                return Err(io::Error::new(
                    ErrorKind::WriteZero,
                    "failed to write whole buffer",
                ))
            }
            Ok(n) => {
                has_interrupted_retry = false;
                size += n;
                buf = &buf[n..]
            }
            Err(ref e) if e.kind() == ErrorKind::Interrupted => {
                if has_interrupted_retry {
                    break;
                }
                has_interrupted_retry = true;
                continue; // 非致命错误，等一下重试
            }
            Err(ref e) if e.kind() == ErrorKind::WouldBlock => break,
            Err(e) => return Err(e),
        }
    }
    Ok(size)
}
