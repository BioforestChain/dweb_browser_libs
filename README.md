# dweb_browser_libs

dweb_browser static library reference.

### Add Rust Target

```shell
#android
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

#ios
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim

#macos
rustup target add aarch64-apple-darwin x86_64-apple-darwin

#windows
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc
brew install mingw-w64
```

### Build Android

```shell
cargo install cbindgen
cargo install cargo-ndk
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release
cargo ndk -t armv7-linux-androideabi -o ../src/androidMain/jniLibs build --release
cargo ndk -t i686-linux-android -o ../src/androidMain/jniLibs build --release
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release
```

### Build iOS

```shell
cargo build --release --target aarch64-apple-ios
cargo build --release --target x86_64-apple-ios
cargo build --release --target aarch64-apple-ios-sim
```

### Build MacOS

```shell
cargo build --release --target aarch64-apple-darwin
cargo build --release --target x86_64-apple-darwin
```

### Build Window

```shell
cargo build --release --target x86_64-pc-windows-msvc
cargo build --release --target aarch64-pc-windows-msvc
```

### Build Linux

> Build for you self XD.
