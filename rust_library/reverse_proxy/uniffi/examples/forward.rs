use rcgen::Certificate;
use reverse_proxy::tls_server::{OpenConnection, TlsServer};
use rustls::PrivateKey;
use std::sync::Arc;

use mio::net::{TcpListener, TcpStream};
use rcgen::generate_simple_self_signed;

#[macro_use]
extern crate log;

use std::collections::HashMap;
use std::fs;
use std::io;
use std::io::{BufReader, Read, Write};
use std::net;

#[macro_use]
extern crate serde_derive;

use docopt::Docopt;

use rustls::server::{
    AllowAnyAnonymousOrAuthenticatedClient, AllowAnyAuthenticatedClient, NoClientAuth,
    UnparsedCertRevocationList,
};
use rustls::{self, RootCertStore};

// Token for our listening socket.
const USAGE: &str = "
Runs a TLS server on :PORT.  The default PORT is 443.

the server forwards plaintext to a connection made to
localhost:fport.

Usage:
  forward [options]
  forward (--help | -h)

Options:
    -p, --port PORT     Listen on PORT [default: 443].
    -f, --forward PORT  Listen on PORT [default: 9999].
    --help, -h          Show this screen.
";

#[derive(Debug, Deserialize)]
struct Args {
    flag_port: Option<u16>,
    flag_forward: Option<u16>,
}

fn main() {
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
    let privkey = rustls::PrivateKey(private_key_der);
    let certs = vec![rustls::Certificate(cert_der)];

    TlsServer::forward(
        args.flag_port.unwrap_or(443),
        args.flag_forward.unwrap_or(9999),
        privkey,
        certs,
    );
}
