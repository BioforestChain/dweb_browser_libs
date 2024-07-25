# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../compression.def ../src/nativeInterop/cinterop/

mkdir -p ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/debug/libcompression.a ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/debug/libcompression.dylib ../src/desktopMain/resources/darwin-aarch64/libcompression.dylib
