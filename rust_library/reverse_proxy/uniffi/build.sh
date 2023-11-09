rm -rf ../src/androidMain

echo "cargo building aarch64-linux-android..."
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building armv7-linux-androideabi..."
cargo ndk -t armv7-linux-androideabi -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building i686-linux-android..."
cargo ndk -t i686-linux-android -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building x86_64-linux-android..."
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release --quiet

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios --release --quiet
echo "cargo building x86_64-apple-ios..."
cargo build --target x86_64-apple-ios --release --quiet
echo "cargo building aarch64-apple-ios-sim..."
cargo build --target aarch64-apple-ios-sim --release --quiet

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../reverse_proxy.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libreverse_proxy.a ../src/libs/iosArm64/
mkdir -p ../src/libs/iosSimulatorArm64/
cp ./target/aarch64-apple-ios-sim/release/libreverse_proxy.a ../src/libs/iosSimulatorArm64/
mkdir -p ../src/libs/iosX64/
cp ./target/x86_64-apple-ios/release/libreverse_proxy.a ../src/libs/iosX64/
