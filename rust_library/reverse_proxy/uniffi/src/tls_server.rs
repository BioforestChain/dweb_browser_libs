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

use crate::open_connection::OpenConnection;

/// This binds together a TCP listening socket, some outstanding
/// connections, and a TLS server configuration.
// #[derive(Clone)]
pub struct TlsServer {
    server: TcpListener,
    pub(crate) connections: HashMap<mio::Token, OpenConnection>,
    next_id: usize,
    tls_config: Arc<rustls::ServerConfig>,
    pub forward: u16,
}
unsafe impl Send for TlsServer {}

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
    pub async fn new<F>(
        port: u16,
        forward: u16,
        privkey: PrivateKey,
        certs: Vec<Certificate>,
        on_listen: F,
    ) -> Self
    where
        F: Future + Send + 'static,
    {
        let addr: net::SocketAddr = format!("0.0.0.0:{}", port).parse().unwrap();
        println!("reverse_proxy forward {addr}");

        let config = TlsServer::make_config(privkey, certs);

        let listener = TcpListener::bind(addr).expect("cannot listen on port");

        let tlsserv = Self {
            server: listener,
            connections: HashMap::new(),
            next_id: 2,
            tls_config: config,
            forward: forward,
        };
        on_listen.await;
        return tlsserv;
    }

    pub async fn listen(&mut self) {
        let mut poll = mio::Poll::new().unwrap();
        poll.registry()
            .register(
                &mut self.server,
                LISTENER,
                mio::Interest::READABLE | mio::Interest::WRITABLE,
            )
            .unwrap();
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
                        self.accept(poll.registry())
                            .expect("error accepting socket");
                    }
                    _ => self.conn_event(
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

fn interrupted(err: &io::Error) -> bool {
    err.kind() == io::ErrorKind::Interrupted
}
