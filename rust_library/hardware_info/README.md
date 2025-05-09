# hardware_info

| Platform | Supported |
| -------- | -------- |
| Linux    | x        |
| Windows  | ✓        |
| macOS    | x        |
| Android  | x        |
| iOS      | x        |

### rust target add

```shell
# windows 不使用GNU的target是因为在各自平台编译能够使包体积更小
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc

# windows GNU 如果想要跨平台编译
rustup target add aarch64-pc-windows-gnullvm x86_64-pc-windows-gnu 
```

### bindings
#### if MacOS
1. install mingw-w64
```shell
brew install mingw-w64
```
2. config cargo
```
# 在 ~/.cargo/config.toml 中添加如下内容：
[target.x86_64-pc-windows-gnu]
linker = "x86_64-w64-mingw32-gcc"
```

```shell
cd hardware_info

../gradlew gen-bindings
```

### cleanup
```shell
cd hardware_info

# only clean bindings
../gradlew cleanup-bindings

# only clean rust targets
../gradlew cleanup-targets

# clean all
../gradlew cleanup-all
```
