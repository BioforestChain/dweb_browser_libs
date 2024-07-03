// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
  name: "keychainstore-swift",
  platforms: [.macOS(.v13), .iOS(.v17)],
  products: [
    // Products define the executables and libraries a package produces, making them visible to other packages.
    .library(
      name: "keychainstore-swift",
      type: .static,
      targets: ["keychainstore-swift"])
  ],
  dependencies: [
    .package(url: "https://github.com/Brendonovich/swift-rs", from: "1.0.6")
  ],
  targets: [
    // Targets are the basic building blocks of a package, defining a module or a test suite.
    // Targets can depend on other targets in this package and products from dependencies.
    .target(
      name: "keychainstore-swift",
      dependencies: [.product(name: "SwiftRs", package: "swift-rs")]
    ),
    .testTarget(
      name: "keychainstore-swiftTests",
      dependencies: ["keychainstore-swift"]),
  ]
)
