# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --release --quiet
echo "cargo building x86_64-apple-darwin..."
cargo build --target x86_64-apple-darwin --release --quiet

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../resvg_render.def ../src/nativeInterop/cinterop/

rm -r ../src/desktopMain/resources/darwin-aarch64/
mkdir -p ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libresvg_render.dylib ../src/desktopMain/resources/darwin-aarch64/libresvg_render.dylib
rm -r ../src/desktopMain/resources/darwin-x86-64/
mkdir -p ../src/desktopMain/resources/darwin-x86-64/
cp -r ./target/x86_64-apple-darwin/release/libresvg_render.dylib ../src/desktopMain/resources/darwin-x86-64/

# android
rm -rf ../src/androidMain

echo "cargo building aarch64-linux-android..."
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building armv7-linux-androideabi..."
cargo ndk -t armv7-linux-androideabi -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building i686-linux-android..."
cargo ndk -t i686-linux-android -o ../src/androidMain/jniLibs build --release --quiet
echo "cargo building x86_64-linux-android..."
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release --quiet

# double build，for uniffi_bindgen::generate_external_bindings
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release --quiet

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

# ios
echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios --release --quiet
echo "cargo building x86_64-apple-ios..."
cargo build --target x86_64-apple-ios --release --quiet
echo "cargo building aarch64-apple-ios-sim..."
cargo build --target aarch64-apple-ios-sim --release --quiet

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../resvg_render.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libresvg_render.a ../src/libs/iosArm64/
mkdir -p ../src/libs/iosSimulatorArm64/
cp ./target/aarch64-apple-ios-sim/release/libresvg_render.a ../src/libs/iosSimulatorArm64/
mkdir -p ../src/libs/iosX64/
cp ./target/x86_64-apple-ios/release/libresvg_render.a ../src/libs/iosX64/

# windows

echo "cargo building x86_64-pc-windows-msvc..."
cargo build --target x86_64-pc-windows-msvc --release --quiet
echo "cargo building aarch64-pc-windows-msvc..."
cargo build --target aarch64-pc-windows-msvc --release --quiet

mkdir -p ../src/desktopMain/resources/win32-x86-64/
cp -r ./target/x86_64-pc-windows-msvc/release/resvg_render.dll ../src/desktopMain/resources/win32-x86-64/
mkdir -p ../src/desktopMain/resources/win32-aarch64/
cp -r ./target/aarch64-pc-windows-msvc/release/resvg_render.dll ../src/desktopMain/resources/win32-aarch64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
