use hyper::body::Body;
use hyper::server::conn::AddrIncoming;
use hyper::service::{make_service_fn, service_fn};
use hyper::{Client, Request, Response, Server};
use hyper_rustls::TlsAcceptor;
use rustls_pemfile::certs;
use std::io::{BufReader, Cursor};
use std::net::SocketAddr;
use std::{env, io};
use tokio::runtime::Builder;

async fn proxy_service(req: Request<Body>) -> Result<Response<Body>, hyper::Error> {
    let client = Client::new();
    let uri = format!(
        "http://localhost:3000{}",
        req.uri().path_and_query().unwrap()
    );
    let headers = req.headers().clone();

    let mut forwarded_req = Request::builder()
        .method(req.method())
        .uri(uri)
        .version(req.version())
        .body(req.into_body())
        .unwrap();

    if headers.len() > 0 {
        for (k, v) in headers {
            forwarded_req.headers_mut().append(k.unwrap(), v.to_owned());
        }
    }

    client.request(forwarded_req).await
}

async fn start_proxy_server(port: u16) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // load the certificates
    let certs = load_certs("path_to_your_cert.pem");
    let key = load_private_key("path_to_your_key.pem");

    let addr = SocketAddr::from(([127, 0, 0, 1], port));
    // Create a TCP listener via tokio.
    let incoming = AddrIncoming::bind(&addr)?;
    let acceptor = TlsAcceptor::builder()
        .with_single_cert(certs, key)
        .map_err(|e| error(format!("{}", e)))?
        .with_all_versions_alpn()
        .with_incoming(incoming);

    let make_svc =
        make_service_fn(|_conn| async { Ok::<_, hyper::Error>(service_fn(proxy_service)) });
    let server = Server::builder(acceptor).serve(make_svc);

    server.await?;
    Ok(())
}

fn error(err: String) -> io::Error {
    io::Error::new(io::ErrorKind::Other, err)
}

pub fn load_certs(data: &str) -> Vec<rustls::Certificate> {
    certs(&mut BufReader::new(Cursor::new(data)))
        .unwrap()
        .into_iter()
        .map(rustls::Certificate)
        .collect()
}

pub fn load_private_key(key: &str) -> rustls::PrivateKey {
    let mut reader = BufReader::new(Cursor::new(key));
    loop {
        match rustls_pemfile::read_one(&mut reader).expect("cannot parse private key .pem file") {
            Some(rustls_pemfile::Item::RSAKey(key)) => return rustls::PrivateKey(key),
            Some(rustls_pemfile::Item::PKCS8Key(key)) => return rustls::PrivateKey(key),
            Some(rustls_pemfile::Item::ECKey(key)) => return rustls::PrivateKey(key),
            None => break,
            _ => {}
        }
    }
    panic!("no keys found in {:?} (encrypted keys not supported)", key);
}

fn start(port: u16) -> () {
    let rt = Builder::new_current_thread().enable_all().build().unwrap();
    _ = rt.block_on(start_proxy_server(port));
}

include!(concat!(env!("OUT_DIR"), "/httpsproxy.uniffi.rs"));
