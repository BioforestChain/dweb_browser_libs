# dweb_browser_libs

dweb_browser static library reference.

## 如何创建一个新的绑定

1. 复制`keychainstore`项目重命名为自己需要创建的绑定。
2. 删除里面的`build`、`src` 文件夹。
3. 按需修改 `build-mobile.gradle.kts` 和 `build.gradle.kts` 修改成跟自己项目相关的。
4. `.def` 文件内容是到`ios` 静态链接库的绑定，如果不是给 `ios`创建绑定可以不管。
5. 按照自己的要求修改`uniffi` 文件夹里的 rust 代码去实现特定的功能。
6. `uniffi`文件夹内部的`.sh`、`toml`文件内容同步成自己的项目名。
7. `uniffi/src` 下的`.udl`文件则是暴露给 kotlin 的调用入口，修改成自己实现的`API`就可以。
8. 如果在`windows` 平台的代码写完了，可以在`windows-lib/examples`先进行测试，每个平台的绑定测试通过了再进行编译。
9. 运行`build.sh`可以自动化生成 rust 绑定的 kotlin 代码。
10. 所有的测试通过并且编译完成之后可以运行`Gradle Sync`,同步完就可以在对应目标引入绑定的项目了`implementation(projects.libKeychainstore)`

> 关于更细节的 windows/apple rust 代码的绑定如何实现，可以参考文件夹内其他项目。

> 如果有时候编译不出来，可以修改一下`build.rs`里的内容，让其产生变更在进行编译。

- [uniffi 文档](https://mozilla.github.io/uniffi-rs/latest/)
- [swift-rs 文档](https://docs.rs/crate/swift-rs)

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

### build dynamic link library

只需要运行每个`uniffi`下的`build-*.sh` 文件即可编译成各个平台的动态链接库。

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

## publish maven

### 发布到本地

到 ./gradelew 目录下运行：

```bash
./gradlew publishToMavenLocal
```

发布后会在本地的`~/.m2` 生成包。项目使用在`setting.gradle.kts` 设置 `mavenLocal()`就能快速调试。
