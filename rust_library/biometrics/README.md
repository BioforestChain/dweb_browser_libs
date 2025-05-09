# biometrics

| Platform | Supported |
| -------- | -------- |
| Linux    | x        |
| Windows  | ✓        |
| macOS    | ✓        |
| Android  | x        |
| iOS      | ✓        |

### rust target add

```shell
# macos
rustup target add aarch64-apple-darwin x86_64-apple-darwin

# windows 不使用GNU的target是因为在各自平台编译能够使包体积更小
rustup target add x86_64-pc-windows-msvc aarch64-pc-windows-msvc

# windows GNU 如果想要跨平台编译
rustup target add aarch64-pc-windows-gnullvm x86_64-pc-windows-gnu 
```

### bindings
```shell
cd biometrics
../gradlew gen-bindings
```

### cleanup
```shell
cd biometrics

# only clean bindings
../gradlew cleanup-bindings

# only clean rust targets
../gradlew cleanup-targets

# clean all
../gradlew cleanup-all
```
