use swift_rs::SwiftLinker;

fn main() {
    SwiftLinker::new("13")
        .with_package("smartscan-swift", "./smartscan-swift/")
        .link();
}
