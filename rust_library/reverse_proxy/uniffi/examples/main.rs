use reverse_proxy::start;

fn main() {
    start(
        8888,
        13377,
        "assets/sample.pem".to_owned(),
        "assets/sample.rsa".to_owned(),
        8000,
    )
}
