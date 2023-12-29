#[macro_use]
extern crate log;

use futures_util::Future;
use hyper::client::HttpConnector;

use rand::{thread_rng, Rng};
use rcgen::generate_simple_self_signed;
use std::convert::Infallible;
use std::net::TcpListener;
use std::vec::Vec;
use tokio::join;
use tokio::sync::oneshot;

use hyper::service::{make_service_fn, service_fn};
use hyper::upgrade::Upgraded;
use hyper::{Body, Client, Method, Request, Response, Server};

use tokio::net::TcpStream;

use crate::tls_server::TlsServer;

type HttpClient = Client<HttpConnector>;

pub mod tls_server;

#[cfg(target_os = "android")]
extern crate android_logger;
#[cfg(target_os = "android")]
use android_logger::{Config, FilterBuilder};
#[cfg(target_os = "android")]
use log::LevelFilter;

#[cfg(target_os = "android")]
fn init_log() {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Trace) // limit log level
            .with_tag("mytag") // logs will show under mytag tag
            .with_filter(
                // configure messages for specific crate
                FilterBuilder::new()
                    .parse("debug,hello::crate=error")
                    .build(),
            ),
    );
}
#[cfg(not(target_os = "android"))]
fn init_log() {}

// #[uniffi::export]
#[tokio::main]
pub async fn start(backend_port: u16, on_ready: Box<dyn VoidCallback>) {
    init_log();
    let frontend_port = find_free_port();

    let (proxy_tx, proxy_rx) = oneshot::channel::<u16>();
    let (frontend_tx, frontend_rx) = oneshot::channel::<u16>();

    join!(
        async move {
            let frontend_port = frontend_rx.await.unwrap();
            let proxy_port = proxy_rx.await.unwrap();
            on_ready.callback(proxy_port, frontend_port);
        },
        run_proxy_server(frontend_port, proxy_tx),
        run_frontend_server(frontend_port, backend_port, async move {
            frontend_tx.send(frontend_port).unwrap();
        }),
    );
}

fn find_free_port() -> u16 {
    let listener = TcpListener::bind("127.0.0.1:0").unwrap();
    let port = listener.local_addr().unwrap().port();
    drop(listener);
    port
}

async fn run_frontend_server<F>(frontend_port: u16, backend_port: u16, on_listen: F)
where
    F: Future + Send + 'static,
{
    let subject_alt_names = vec!["localhost.dweb".to_string()];

    let cert = generate_simple_self_signed(subject_alt_names).unwrap();

    let cert_der = cert.serialize_der().unwrap();
    let private_key_der = cert.serialize_private_key_der();
    let private_key = rustls::PrivateKey(private_key_der);
    let cert_chain = vec![rustls::Certificate(cert_der)];

    // Create a tls forward server.
    tokio::spawn(async move {
        TlsServer::forward(
            frontend_port,
            backend_port,
            private_key,
            cert_chain,
            on_listen,
        )
        .await;
    });
    // Run the future, keep going until an error occurs.
    info!(
        "frontend serve listening on https://0.0.0.0:{} => backend server http://127.0.0.1:{}",
        frontend_port, backend_port
    );
}

// async fn run_proxy_server(proxy_port: u16, frontend_port: u16) -> bool {
async fn run_proxy_server(frontend_port: u16, tx: oneshot::Sender<u16>) {
    let addr = format!("0.0.0.0:{}", find_free_port()).parse().unwrap();

    let client = Client::builder()
        .http1_title_case_headers(true)
        .http1_preserve_header_case(true)
        .build_http();

    let make_service = make_service_fn(move |_| {
        let client = client.clone();
        async move {
            Ok::<_, Infallible>(service_fn(move |req| {
                proxy(client.clone(), req, frontend_port)
            }))
        }
    });

    let server = Server::bind(&addr)
        .http1_preserve_header_case(true)
        .http1_title_case_headers(true)
        .serve(make_service);

    let (listen_tx, listen_rx) = oneshot::channel::<()>();

    if let Err(e) = server
        .with_graceful_shutdown(async move {
            tx.send(addr.port()).unwrap();
            info!("proxy server listening on http://{}", addr);
            listen_rx.await.unwrap();
            info!("proxy server closed");
        })
        .await
    {
        error!("start proxy server error: {}", e);
        listen_tx.send(()).unwrap();
    }
}

async fn proxy(
    client: HttpClient,
    req: Request<Body>,
    frontend_port: u16,
) -> Result<Response<Body>, hyper::Error> {
    debug!("req: {:?}", req);
    let frontend_addr = format!("127.0.0.1:{}", frontend_port);

    if Method::CONNECT == req.method() {
        // Received an HTTP request like:
        // ```
        // CONNECT www.domain.com:443 HTTP/1.1
        // Host: www.domain.com:443
        // Proxy-Connection: Keep-Alive
        // ```
        //
        // When HTTP method is CONNECT we should return an empty body
        // then we can eventually upgrade the connection and talk a new protocol.
        //
        // Note: only after client received an empty body with STATUS_OK can the
        // connection be upgraded, so we can't return a response inside
        // `on_upgrade` future.
        if let Some(addr) = host_addr(req.uri()) {
            tokio::task::spawn(async move {
                match hyper::upgrade::on(req).await {
                    Ok(upgraded) => {
                        if addr.ends_with(".dweb:443") {
                            info!("proxy: {} -> {}", addr, frontend_addr);
                            if let Err(e) = tunnel(upgraded, frontend_addr.to_owned()).await {
                                error!("server io error: {}", e);
                            };
                        } else {
                            info!("direct: {}", addr);
                            match TcpStream::connect(addr).await {
                                Ok(target_stream) => {
                                    let (mut client_reader, mut client_writer) =
                                        tokio::io::split(upgraded);

                                    // 获取目标服务器流
                                    let (mut server_reader, mut server_writer) =
                                        target_stream.into_split();

                                    // 使用Tokio的spawn创建新的任务来处理两个流之间的数据转发
                                    let client_to_server =
                                        tokio::io::copy(&mut client_reader, &mut server_writer);
                                    let server_to_client =
                                        tokio::io::copy(&mut server_reader, &mut client_writer);

                                    // 使用tokio::try_join!等待两个数据流转发完成
                                    let _ = tokio::try_join!(client_to_server, server_to_client);
                                }
                                Err(e) => {
                                    error!("Failed to connect to target: {}", e);
                                }
                            }
                        }
                    }
                    Err(e) => {
                        error!("upgrade error: {}", e);
                    }
                }
            });
            Ok(Response::new(Body::empty()))
        } else {
            error!("CONNECT host is not socket addr: {:?}", req.uri());
            let mut resp = Response::new(Body::from("CONNECT must be to a socket address"));
            *resp.status_mut() = http::StatusCode::BAD_REQUEST;

            Ok(resp)
        }
    } else {
        client.request(req).await
    }
}

fn host_addr(uri: &http::Uri) -> Option<String> {
    uri.authority().and_then(|auth| Some(auth.to_string()))
}

// Create a TCP connection to host:port, build a tunnel between the connection and
// the upgraded connection
async fn tunnel(mut upgraded: Upgraded, addr: String) -> std::io::Result<()> {
    // Connect to remote server
    info!("TcpStream::connect: {}", addr);
    let mut server = TcpStream::connect(addr).await?;
    info!("tunnel server: {}", server.peer_addr()?);

    // Proxying data
    let (from_client, from_server) =
        tokio::io::copy_bidirectional(&mut upgraded, &mut server).await?;

    // Print message when done
    debug!(
        "client wrote {} bytes and received {} bytes",
        from_client, from_server
    );

    Ok(())
}

pub trait VoidCallback: Send + Sync + std::fmt::Debug {
    fn callback(&self, proxy_port: u16, frontend_port: u16);
}

uniffi::include_scaffolding!("reverse_proxy");
