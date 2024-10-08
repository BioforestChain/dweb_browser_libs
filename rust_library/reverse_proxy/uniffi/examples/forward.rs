use std::borrow::BorrowMut;

use reverse_proxy::tls_server::{self, TlsServer};

use rcgen::generate_simple_self_signed;

#[macro_use]
extern crate log;

#[macro_use]
extern crate serde_derive;

use docopt::Docopt;

use rustls::{self};

// Token for our listening socket.
const USAGE: &str = "
Runs a TLS server on :PORT.  The default PORT is 443.

the server forwards plaintext to a connection made to
localhost:fport.

Usage:
  forward [options]
  forward (--help | -h)

Options:
    -p, --port PORT     Listen on PORT [default: 1443].
    -f, --forward PORT  Forward on PORT [default: 8000].
    --help, -h          Show this screen.
";

#[derive(Debug, Deserialize)]
struct Args {
    flag_port: Option<u16>,
    flag_forward: Option<u16>,
}

#[tokio::main]
async fn main() {
    let version = env!("CARGO_PKG_NAME").to_string() + ", version: " + env!("CARGO_PKG_VERSION");

    let args: Args = Docopt::new(USAGE)
        .map(|d| d.help(true))
        .map(|d| d.version(Some(version)))
        .and_then(|d| d.deserialize())
        .unwrap_or_else(|e| e.exit());

    let subject_alt_names = vec!["localhost.dweb".to_string()];
    let cert = generate_simple_self_signed(subject_alt_names).unwrap();

    let cert_der = cert.serialize_der().unwrap();
    let private_key_der = cert.serialize_private_key_der();
    let private_key = rustls::PrivateKey(private_key_der);
    let cert_chain = vec![rustls::Certificate(cert_der)];

    env_logger::init();

    println!("args.flag_port={:?}", args.flag_port);

    let mut tls_server = TlsServer::new(
        args.flag_port.unwrap_or(1443),
        args.flag_forward.unwrap_or(8000),
        private_key,
        cert_chain,
        async {
            println!("server started okk~");
        },
    )
    .await;
    tls_server.listen().await;
}
