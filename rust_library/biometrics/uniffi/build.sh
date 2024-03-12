rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../biometrics.def ../src/nativeInterop/cinterop/

# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --release --quiet
echo "cargo building x86_64-apple-darwin..."
cargo build --target x86_64-apple-darwin --release --quiet

mkdir -p ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libbiometrics.a ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libbiometrics.dylib ../src/desktopMain/resources/darwin-aarch64/libbiometrics.dylib
mkdir -p ../src/desktopMain/resources/darwin-x86_64/
cp -r ./target/x86_64-apple-darwin/release/libbiometrics.a ../src/desktopMain/resources/darwin-x86_64/
cp -r ./target/x86_64-apple-darwin/release/libbiometrics.dylib ../src/desktopMain/resources/darwin-x86_64/

# windows

echo "cargo building x86_64-pc-windows-gnu..."
cargo build --target x86_64-pc-windows-gnu --release --quiet

mkdir -p ../src/desktopMain/resources/windows-x86_64/
cp -r ./target/x86_64-pc-windows-gnu/release/biometrics.dll ../src/desktopMain/resources/windows-x86_64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
