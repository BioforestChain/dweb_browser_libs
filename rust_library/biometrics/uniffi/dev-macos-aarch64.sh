# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../biometrics.def ../src/nativeInterop/cinterop/

mkdir -p ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/debug/libbiometrics.a ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/debug/libbiometrics.dylib ../src/desktopMain/resources/darwin-aarch64/libbiometrics.dylib
