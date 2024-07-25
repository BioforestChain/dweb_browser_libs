# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --release
echo "cargo building x86_64-apple-darwin..."
cargo build --target x86_64-apple-darwin --release

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../mix_compression.def ../src/nativeInterop/cinterop/

rm -r ../src/desktopMain/resources/darwin-aarch64/
mkdir -p ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libmix_compression.dylib ../src/desktopMain/resources/darwin-aarch64/libmix_compression.dylib
rm -r ../src/desktopMain/resources/darwin-x86-64/
mkdir -p ../src/desktopMain/resources/darwin-x86-64/
cp -r ./target/x86_64-apple-darwin/release/libmix_compression.dylib ../src/desktopMain/resources/darwin-x86-64/

# android
rm -rf ../src/androidMain

echo "cargo building aarch64-linux-android..."
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release
echo "cargo building armv7-linux-androideabi..."
cargo ndk -t armv7-linux-androideabi -o ../src/androidMain/jniLibs build --release
echo "cargo building i686-linux-android..."
cargo ndk -t i686-linux-android -o ../src/androidMain/jniLibs build --release
echo "cargo building x86_64-linux-android..."
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release

# double build，for uniffi_bindgen::generate_external_bindings
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

# ios
echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios --release
echo "cargo building x86_64-apple-ios..."
cargo build --target x86_64-apple-ios --release
echo "cargo building aarch64-apple-ios-sim..."
cargo build --target aarch64-apple-ios-sim --release

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../mix_compression.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libmix_compression.a ../src/libs/iosArm64/
mkdir -p ../src/libs/iosSimulatorArm64/
cp ./target/aarch64-apple-ios-sim/release/libmix_compression.a ../src/libs/iosSimulatorArm64/
mkdir -p ../src/libs/iosX64/
cp ./target/x86_64-apple-ios/release/libmix_compression.a ../src/libs/iosX64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain

# # windows

# echo "cargo building x86_64-pc-windows-msvc..."
# cargo build --target x86_64-pc-windows-msvc --release
# echo "cargo building aarch64-pc-windows-msvc..."
# cargo build --target aarch64-pc-windows-msvc --release

# mkdir -p ../src/desktopMain/resources/win32-x86-64/
# cp -r ./target/x86_64-pc-windows-msvc/release/mix_compression.dll ../src/desktopMain/resources/win32-x86-64/
# mkdir -p ../src/desktopMain/resources/win32-aarch64/
# cp -r ./target/aarch64-pc-windows-msvc/release/mix_compression.dll ../src/desktopMain/resources/win32-aarch64/

# # jvm
# cp -r ./target/bindings/jvmMain/ ../src/desktopMain
