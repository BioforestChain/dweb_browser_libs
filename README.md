# dweb_browser_libs

dweb_browser static library reference.

### Add Rust Target

```shell
# android
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

# ios
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim

# macos
rustup target add aarch64-apple-darwin x86_64-apple-darwin

# windows on windows
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc
# [install winget](https://learn.microsoft.com/en-us/windows/package-manager/winget/)
winget install Microsoft.VisualStudio.BuildTools

# windows on macos/linux
rustup target add x86_64-pc-windows-gnu

```

### Install toolchains on MacOS

1. 下载并解压 https://github.com/mstorsjo/llvm-mingw/releases/download/20240619/llvm-mingw-20240619-ucrt-macos-universal.tar.xz
1. 到 `~/.cargo/config.toml` 中修改

   ```toml
   [target.x86_64-pc-windows-gnu]
   linker = "x86_64-w64-mingw32-gcc"

   [target.aarch64-pc-windows-gnullvm]
   linker = "PATH_TO_LLVM_MINGW_UCRT_MACOS_UNIVERSAL/bin/aarch64-w64-mingw32-clang"
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
