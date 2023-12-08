use reverse_proxy::{start, VoidCallback};

#[derive(Debug, Default)]
struct MyCallback;

impl VoidCallback for MyCallback {
    fn callback(&self, proxy_port: u16, frontend_port: u16) {
        println!("callback {} {}", proxy_port, frontend_port);
    }
}

fn main() {
    let callback = MyCallback::default();

    start(8000, Box::new(callback))
}
