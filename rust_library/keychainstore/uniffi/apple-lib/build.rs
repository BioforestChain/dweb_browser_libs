use swift_rs::SwiftLinker;

fn main() {
    SwiftLinker::new("13")
        .with_ios("17")
        .with_package("keychainstore-swift", "./keychainstore-swift/")
        .link();
}
