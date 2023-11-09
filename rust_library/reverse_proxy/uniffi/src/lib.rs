use hyper::client::HttpConnector;
use std::convert::Infallible;
use std::net::SocketAddr;
use std::vec::Vec;
use std::{fs, future, io};
use tokio::join;

use hyper::server::conn::{AddrIncoming, Http};
use hyper::service::{make_service_fn, service_fn};
use hyper::upgrade::Upgraded;
use hyper::{Body, Client, Method, Request, Response, Server};
use hyper_rustls::TlsAcceptor;

use tokio::net::TcpStream;

type HttpClient = Client<hyper::client::HttpConnector>;

pub fn add(a: i32, b: i32) -> i32 {
    return a + b;
}

// #[uniffi::export]
#[tokio::main]
pub async fn start(
    proxy_port: u16,
    frontend_port: u16,
    frontend_certs_path: String,
    frontend_key_path: String,
    backend_port: u16,
) {
    // RT.block_on(async move {
    join!(
        run_proxy_server(proxy_port, frontend_port),
        run_frontend_server(
            frontend_port,
            &frontend_certs_path,
            &frontend_key_path,
            backend_port,
        ),
    );
    // });
}

fn error(err: String) -> io::Error {
    io::Error::new(io::ErrorKind::Other, err)
}

async fn run_frontend_server(
    frontend_port: u16,
    certs_path: &str,
    key_path: &str,
    backend_port: u16,
) {
    let addr = SocketAddr::from(([0, 0, 0, 0], frontend_port)); // 13377

    // Load public certificate.
    let certs = load_certs(certs_path).unwrap();
    // Load private key.
    let key = load_private_key(key_path).unwrap();
    // Build TLS configuration.

    // Create a TCP listener via tokio.
    let incoming = AddrIncoming::bind(&addr).unwrap();
    let acceptor = TlsAcceptor::builder()
        .with_single_cert(certs, key)
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
                let uri = uri_string.parse().unwrap();
                *req.uri_mut() = uri;
                client.request(req)
            }))
        }
    }));

    // Run the future, keep going until an error occurs.
    println!(
        "frontend serve listening on https://{} => backend server http://127.0.0.1:{}",
        addr, backend_port
    );
    if let Err(e) = make_server.await {
        eprintln!("start frontend server error: {}", e);
    };
}

// Load public certificate from file.
fn load_certs(filename: &str) -> io::Result<Vec<rustls::Certificate>> {
    // Open certificate file.
    let certfile = fs::File::open(filename)
        .map_err(|e| error(format!("failed to open {}: {}", filename, e)))?;
    let mut reader = io::BufReader::new(certfile);

    // Load and return certificate.
    let certs = rustls_pemfile::certs(&mut reader)
        .map_err(|_| error("failed to load certificate".into()))?;
    Ok(certs.into_iter().map(rustls::Certificate).collect())
}

// Load private key from file.
fn load_private_key(filename: &str) -> io::Result<rustls::PrivateKey> {
    // Open keyfile.
    let keyfile = fs::File::open(filename)
        .map_err(|e| error(format!("failed to open {}: {}", filename, e)))?;
    let mut reader = io::BufReader::new(keyfile);

    // Load and return a single private key.
    let keys = rustls_pemfile::rsa_private_keys(&mut reader)
        .map_err(|_| error("failed to load private key".into()))?;
    if keys.len() != 1 {
        return Err(error("expected a single private key".into()));
    }

    Ok(rustls::PrivateKey(keys[0].clone()))
}

async fn run_proxy_server(proxy_port: u16, frontend_port: u16) {
    let addr = SocketAddr::from(([0, 0, 0, 0], proxy_port)); // 8888

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

uniffi::include_scaffolding!("reverse_proxy");
