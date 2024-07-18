# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --release --quiet
echo "cargo building x86_64-apple-darwin..."
cargo build --target x86_64-apple-darwin --release --quiet

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain
# jvm
rm -rf ../src/desktopMain
cp -r ./target/bindings/jvmMain/ ../src/desktopMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../biometrics.def ../src/nativeInterop/cinterop/

mkdir -p ../src/desktopMain/resources/darwin-aarch64/
# cp -r ./target/aarch64-apple-darwin/release/libbiometrics.a ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libbiometrics.dylib ../src/desktopMain/resources/darwin-aarch64/
mkdir -p ../src/desktopMain/resources/darwin-x86-64/
# cp -r ./target/x86_64-apple-darwin/release/libbiometrics.a ../src/desktopMain/resources/darwin-x86-64/
cp -r ./target/x86_64-apple-darwin/release/libbiometrics.dylib ../src/desktopMain/resources/darwin-x86-64/
