// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "biometrics-swift",
    platforms: [.macOS(.v13)],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "biometrics-swift",
            type: .static,
            targets: ["biometrics-swift"]),
    ],
    dependencies: [
        .package(url: "https://github.com/Brendonovich/swift-rs", from: "1.0.6")
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "biometrics-swift",
            dependencies: [.product(name: "SwiftRs", package: "swift-rs")]
        ),
        .testTarget(
            name: "biometrics-swiftTests",
            dependencies: ["biometrics-swift"]),
    ]
)
