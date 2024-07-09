# ios
echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios --release --quiet

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../keychainstore.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libkeychainstore.a ../src/libs/iosArm64/