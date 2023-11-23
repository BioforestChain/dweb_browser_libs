use hyper::client::HttpConnector;
use rand::{thread_rng, Rng};
use rcgen::generate_simple_self_signed;
use std::convert::Infallible;
use std::io;
use std::net::{IpAddr, Ipv4Addr, SocketAddr, ToSocketAddrs};
use std::vec::Vec;
use tokio::join;
use tokio::sync::oneshot;

use hyper::server::conn::AddrIncoming;
use hyper::service::{make_service_fn, service_fn};
use hyper::upgrade::Upgraded;
use hyper::{Body, Client, Method, Request, Response, Server};
use hyper_rustls::TlsAcceptor;

use tokio::net::TcpStream;

type HttpClient = Client<HttpConnector>;

// #[uniffi::export]
#[tokio::main]
pub async fn start(backend_port: u16, on_ready: Box<dyn VoidCallback>) {
    let front_addr = gen_addr("0.0.0.0");

    let (tx, rx) = oneshot::channel();
    tokio::spawn(async move {
        let proxy_port = rx.await.unwrap();
        on_ready.callback(proxy_port, front_addr.port())
    });

    join!(
        run_proxy_server(front_addr.port(), tx),
        run_frontend_server(&front_addr, backend_port,),
    );
}

fn error(err: String) -> io::Error {
    io::Error::new(io::ErrorKind::Other, err)
}

fn gen_addr(host: &str) -> SocketAddr {
    let addr = host.to_socket_addrs();

    match addr {
        Ok(addrs) => addrs.last().unwrap(),
        Err(_) => {
            let mut rng = thread_rng(); // 创建一个随机数生成器
            let random_port = rng.gen_range(1024..49151);
            SocketAddr::new(IpAddr::V4(host.parse::<Ipv4Addr>().unwrap()), random_port)
        }
    }
}

async fn run_frontend_server(frontend_addr: &SocketAddr, backend_port: u16) {
    let subject_alt_names = vec!["localhost.dweb".to_string()];

    let cert = generate_simple_self_signed(subject_alt_names).unwrap();
    let cert_der = cert.serialize_der().unwrap();
    let private_key_der = cert.serialize_private_key_der();
    let private_key = rustls::PrivateKey(private_key_der);
    let cert_chain = vec![rustls::Certificate(cert_der)];

    // Create a TCP listener via tokio.
    let incoming = AddrIncoming::bind(&frontend_addr).unwrap();
    let acceptor = TlsAcceptor::builder()
        .with_single_cert(cert_chain, private_key)
        .map_err(|e| error(format!("{}", e)))
        .unwrap()
        .with_http11_alpn()
        .with_incoming(incoming);

    let client_main = Client::new();

    let make_server = Server::builder(acceptor).serve(make_service_fn(move |_| {
        let client = client_main.clone();

        async move {
            // This is the `Service` that will handle the connection.
            // `service_fn` is a helper to convert a function that
            // returns a Response into a `Service`.
            Ok::<_, io::Error>(service_fn(move |mut req| {
                let uri_string = format!(
                    "http://127.0.0.1:{backend_port}{}",
                    req.uri()
                        .path_and_query()
                        .map(|x| x.as_str())
                        .unwrap_or("/")
                );
                println!("make_server uri_string: {}", uri_string);
                let uri = uri_string.parse().unwrap();
                *req.uri_mut() = uri;
                client.request(req)
            }))
        }
    }));

    // Run the future, keep going until an error occurs.
    println!(
        "frontend serve listening on https://{} => backend server http://127.0.0.1:{}",
        frontend_addr, backend_port
    );

    if let Err(e) = make_server.await {
        eprintln!("start frontend server error: {}", e);
    };
}

// async fn run_proxy_server(proxy_port: u16, frontend_port: u16) -> bool {
async fn run_proxy_server(frontend_port: u16, tx: oneshot::Sender<u16>) {
    let addr = gen_addr("0.0.0.0");

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

    println!("proxy server listening on http://{}", addr);

    if let Err(e) = tx.send(server.local_addr().port()) {
        eprintln!("start proxy server channel sender error: {}", e);
    }

    if let Err(e) = server.await {
        eprintln!("start proxy server error: {}", e);
    }
}

async fn proxy(
    client: HttpClient,
    req: Request<Body>,
    frontend_port: u16,
) -> Result<Response<Body>, hyper::Error> {
    println!("req: {:?}", req);
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
                        let tunnel_addr = if addr.contains(".dweb") {
                            &frontend_addr //"127.0.0.1:13377"
                        } else {
                            &addr
                        };
                        println!("proxy: {} -> {}", addr, tunnel_addr);
                        if let Err(e) = tunnel(upgraded, tunnel_addr.to_owned()).await {
                            eprintln!("server io error: {}", e);
                        };
                    }
                    Err(e) => eprintln!("upgrade error: {}", e),
                }
            });

            Ok(Response::new(Body::empty()))
        } else {
            eprintln!("CONNECT host is not socket addr: {:?}", req.uri());
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
    println!("TcpStream::connect: {}", addr);
    let mut server = TcpStream::connect(addr).await?;
    println!("tunnel server: {}", server.peer_addr()?);

    // Proxying data
    let (from_client, from_server) =
        tokio::io::copy_bidirectional(&mut upgraded, &mut server).await?;

    // Print message when done
    println!(
        "client wrote {} bytes and received {} bytes",
        from_client, from_server
    );

    Ok(())
}

pub trait VoidCallback: Send + Sync + std::fmt::Debug {
    fn callback(&self, proxy_port: u16, frontend_port: u16);
}

uniffi::include_scaffolding!("reverse_proxy");
