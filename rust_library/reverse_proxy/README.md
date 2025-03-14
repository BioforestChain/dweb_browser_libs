# reverse_proxy

| Platform | Supported |
| -------- | --------- |
| Linux    | x         |
| Windows  | x         |
| macOS    | x         |
| Android  | ✓         |
| iOS      | ✓         |

### rust target add

```shell
# iOS
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim

# android
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
```

### cargo build
```shell
cd ziplib

# iOS
../gradlew build-ios

# Android
../gradlew build-android
```
or
```shell
# macos computer build
../gradlew macos-cargo-build

# windows computer build
../gradlew win-cargo-build
```

### bindings
```shell
cd reverse_proxy
../gradlew prepareKotlinIdeaImport
```

### cleanup
```shell
cd reverse_proxy

# only clean bindings
../gradlew cleanup-bindings

# only clean rust targets
../gradlew cleanup-targets

# clean all
../gradlew cleanup-all
```
