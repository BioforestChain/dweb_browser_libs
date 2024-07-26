# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --release --quiet
echo "cargo building x86_64-apple-darwin..."
cargo build --target x86_64-apple-darwin --release --quiet

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../keychainstore.def ../src/nativeInterop/cinterop/

mkdir -p ../src/desktopMain/resources/darwin-aarch64/
# cp -r ./target/aarch64-apple-darwin/release/libkeychainstore.a ../src/desktopMain/resources/darwin-aarch64/
cp -r ./target/aarch64-apple-darwin/release/libkeychainstore.dylib ../src/desktopMain/resources/darwin-aarch64/libkeychainstore.dylib
mkdir -p ../src/desktopMain/resources/darwin-x86-64/
# cp -r ./target/x86_64-apple-darwin/release/libkeychainstore.a ../src/desktopMain/resources/darwin-x86-64/
cp -r ./target/x86_64-apple-darwin/release/libkeychainstore.dylib ../src/desktopMain/resources/darwin-x86-64/

# ios
echo "cargo building aarch64-apple-ios..."
cargo build --target aarch64-apple-ios --release --quiet
echo "cargo building x86_64-apple-ios..."
cargo build --target x86_64-apple-ios --release --quiet
echo "cargo building aarch64-apple-ios-sim..."
cargo build --target aarch64-apple-ios-sim --release --quiet

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../keychainstore.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libkeychainstore.a ../src/libs/iosArm64/
mkdir -p ../src/libs/iosSimulatorArm64/
cp ./target/aarch64-apple-ios-sim/release/libkeychainstore.a ../src/libs/iosSimulatorArm64/
mkdir -p ../src/libs/iosX64/
cp ./target/x86_64-apple-ios/release/libkeychainstore.a ../src/libs/iosX64/

# windows

echo "cargo building x86_64-pc-windows-msvc..."
cargo build --target x86_64-pc-windows-msvc --release --quiet
echo "cargo building aarch64-pc-windows-msvc..."
cargo build --target aarch64-pc-windows-msvc --release --quiet

mkdir -p ../src/desktopMain/resources/win32-x86-64/
cp -r ./target/x86_64-pc-windows-msvc/release/keychainstore.dll ../src/desktopMain/resources/win32-x86-64/
mkdir -p ../src/desktopMain/resources/win32-aarch64/
cp -r ./target/aarch64-pc-windows-msvc/release/keychainstore.dll ../src/desktopMain/resources/win32-aarch64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
