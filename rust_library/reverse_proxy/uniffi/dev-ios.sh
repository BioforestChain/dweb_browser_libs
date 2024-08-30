cargo build --target aarch64-apple-ios

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../reverse_proxy.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../reverse_proxy.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/debug/libreverse_proxy.a ../src/libs/iosArm64/

