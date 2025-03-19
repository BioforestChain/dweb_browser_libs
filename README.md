# Dweb Browser 跨平台库

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

`Dweb Browser` 跨平台静态库集合，提供多平台原生功能的 `Rust` 实现与 `Kotlin Multiplatform` 绑定。

## 项目概述

本项目提供了一系列跨平台的 `Rust` 实现库，通过 [uniffi](https://mozilla.github.io/uniffi-rs/latest/) 技术将原生功能暴露给 `Kotlin Multiplatform` 使用。主要特点：

- 跨平台支持：Android、iOS、macOS、Windows 等
- 高性能：核心功能使用 Rust 实现
- 安全性：利用 Rust 的内存安全特性
- 易集成：提供标准化的 Kotlin 绑定接口

## 可用模块

| 模块名称 | 功能描述 | Android | iOS | macOS | Windows | Linux |
|---------|---------|:-------:|:---:|:-----:|:-------:|:-----:|
| [biometrics](./rust_library/biometrics) | 生物识别认证 | ❌ | ❌ | ✅ | ✅ | ❌ |
| [hardware_info](./rust_library/hardware_info) | 设备硬件信息 | ❌ | ❌ | ❌ | ✅ | ❌ |
| [keychainstore](./rust_library/keychainstore) | 安全密钥存储 | ❌ | ✅ | ✅ | ✅ | ❌ |
| [mix_compression](./rust_library/mix_compression) | 数据压缩 | ✅ | ✅ | ✅ | ✅ | ❌ |
| [multipart](./rust_library/multipart) | http请求解析multipart | ✅ | ✅ | ✅ | ✅ | ❌ |
| [resvg_render](./rust_library/resvg_render) | svg渲染 | ✅ | ✅ | ✅ | ✅ | ❌ |
| [reverse_proxy](./rust_library/reverse_proxy) | 反向代理 | ✅ | ✅ | ❌ | ❌ | ❌ |
| [ziplib](./rust_library/ziplib) | 解压缩 | ✅ | ✅ | ✅ | ✅ | ❌ |
| ... | ... | ... | ... | ... | ... | ... |

## 添加 Rust Target

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
rustup target add x86_64-pc-windows-gnu aarch64-pc-windows-gnullvm

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

## Publish Maven

### 发布到本地
1. 生成gpg文件
2. 取出 private key
```bash
gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg
```
3. 需要在`local.properties`文件中配置:
```
# 发布到 maven 配置
#####################
# gpg生成的keyId为后8位
signing.keyId=
# gpg文件路径secring.gpg路径
signing.secretKeyRingFile=
# gpg设置的密码
signing.password=

# ossrh http://s01.oss.sonatype.org/ 中的token的username和password
ossrhUsername=
ossrhPassword=
####################

# 发布到 GitHub Package 配置
####################
# github 用户名
githubPackagesUsername=
# github token Personal access tokens (classic)，需要权限 write packages
# see: https://github.com/settings/tokens
githubPackagesPassword=
####################
```

4. 到 `rust_library` 目录下运行：
```bash
./gradlew publishToMavenLocal
```

发布后会在本地的`~/.m2` 生成包。项目使用在`setting.gradle.kts` 设置 `mavenLocal()`就能快速调试。
```kts
dependencyResolutionManagement {
   repositories {
      ...other MavenArtifactRepositories
      mavenLocal()
   }
}
```

### 发布到正式环境
> 使用mac电脑进行发布，否则会缺失 iOS 和 mac 的包
```bash
# 发布到 maven
./gradlew publish

# 发布到 GitHub Packages
./gradlew publish -PTarget=github
```

## 资料

- [Kotlin Multiplatform 文档](https://kotlinlang.org/docs/multiplatform.html)
- [uniffi 文档](https://mozilla.github.io/uniffi-rs/latest/)
- [gobley 文档](https://gobley.dev/)