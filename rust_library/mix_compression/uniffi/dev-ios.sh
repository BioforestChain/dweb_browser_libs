
# ios
echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios
echo "cargo building aarch64-apple-ios-sim..."
cargo build --target aarch64-apple-ios-sim
echo "cargo building x86_64-apple-ios..."
cargo build --target x86_64-apple-ios

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../mix_compression.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/debug/libmix_compression.a ../src/libs/iosArm64/
mkdir -p ../src/libs/iosSimulatorArm64/
cp ./target/aarch64-apple-ios-sim/debug/libmix_compression.a ../src/libs/iosSimulatorArm64/
mkdir -p ../src/libs/iosX64/
cp ./target/x86_64-apple-ios/debug/libmix_compression.a ../src/libs/iosX64/