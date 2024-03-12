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
   cargo run --bin uniffi-bindgen generate src/biometrics.udl --language kotlin --out-dir out
   ```

## 关于开发

1. 开发的时候需要将 cargo.toml 中的 lib 和 bin 注释掉，然后打开第一个 bin 配置
1. `cargo run --example forward` 可以启动 tls 转发服务，默认端口是 1443(tls-server) -> 8000(your-server)
   1. `export RUST_LOG=debug` 可以开启调试日子
1. `deno run -A ./assets/ws.ts` 可以启动一个简易的 http-server，内有 websocket 的支持
1. `curl --insecure -v http://localhost:8000`
   1. `-v` 可以看到详细的请求过程，包括 tls 握手
   1. `--insecure` 可以忽视对于本地自签证书的不信任问题
