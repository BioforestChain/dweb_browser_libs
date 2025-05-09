# resvg_render

| Platform | Supported |
| -------- | --------- |
| Linux    | x         |
| Windows  | ✓         |
| macOS    | ✓         |
| Android  | ✓         |
| iOS      | ✓         |

### rust target add

```shell
# iOS
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim

# android
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

# macos
rustup target add aarch64-apple-darwin x86_64-apple-darwin

# windows 不使用GNU的target是因为在各自平台编译能够使包体积更小
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc

# windows GNU 如果想要跨平台编译
rustup target add aarch64-pc-windows-gnullvm x86_64-pc-windows-gnu 
```

### bindings
```shell
cd resvg_render
../gradlew gen-bindings
```

### cleanup
```shell
cd resvg_render

# only clean bindings
../gradlew cleanup-bindings

# only clean rust targets
../gradlew cleanup-targets

# clean all
../gradlew cleanup-all
```