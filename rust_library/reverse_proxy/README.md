### Prepare Env

```shell
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
cargo install cbindgen
cargo install cargo-ndk
```

### Android & IOS

```shell
./uniffi/build.sh
```

## UNIFFI

1. 关于 kmp 的支持
   目前 uniffi 官方只支持 jvm-koltin 的输出，对于多平台的支持还没做好。
   官方自己文档也是推荐第三方 https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings 来做支持
   所以目前还在等官方的支持

   ```shell
   cargo run --bin uniffi-bindgen generate src/reverse_proxy.udl --language kotlin --out-dir out
   ```

## 关于开发

开发的时候需要将 cargo.toml 中的 lib 和 bin 注释掉，然后打开第一个 bin 配置
