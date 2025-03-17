# keychainstore

| Platform | Supported |
| -------- | -------- |
| Linux    | x        |
| Windows  | ✓        |
| macOS    | ✓        |
| Android  | x        |
| iOS      | ✓        |

### rust target add

```shell
# iOS
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim

# macos
rustup target add aarch64-apple-darwin x86_64-apple-darwin

# windows 不使用GNU的target是因为在各自平台编译能够使包体积更小
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc

# windows GNU 如果想要跨平台编译
rustup target add aarch64-pc-windows-gnullvm x86_64-pc-windows-gnu 
```

### cargo build
```shell
cd keychainstore

# iOS
../gradlew build-ios

# macos
../gradlew build-macos

# windows
## x86-64
../gradlew build-win-x86_64
## arm64
../gradlew build-win-arm64

## cross arch GNU
../gradlew build-win
```
or
```shell
# macos computer build
../gradlew macos-cargo-build

# windows computer build
../gradlew win-cargo-build

# windows cross arch build
../gradlew win-gnu-cargo-build
```

### bindings
```shell
cd keychainstore
../gradlew gen-bindings
```

### cleanup
```shell
cd keychainstore

# only clean bindings
../gradlew cleanup-bindings

# only clean rust targets
../gradlew cleanup-targets

# clean all
../gradlew cleanup-all
```
