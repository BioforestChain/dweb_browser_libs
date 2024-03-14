use swift_rs::SwiftLinker;

fn main() {
    SwiftLinker::new("13")
        .with_package("biometrics-swift", "./biometrics-swift/")
        .link();
}