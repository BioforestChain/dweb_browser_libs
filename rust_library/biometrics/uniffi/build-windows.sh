# windows

echo "cargo building x86_64-pc-windows-gnu..."
cargo build --target x86_64-pc-windows-gnu --release --quiet
echo "cargo building aarch64-pc-windows-msvc..."
cargo build --target aarch64-pc-windows-msvc --release --quiet

mkdir -p ../src/desktopMain/resources/win32-x86-64/
cp -r ./target/x86_64-pc-windows-gnu/release/biometrics.dll ../src/desktopMain/resources/win32-x86-64/
mkdir -p ../src/desktopMain/resources/win32-aarch64/
cp -r ./target/aarch64-pc-windows-msvc/release/biometrics.dll ../src/desktopMain/resources/win32-aarch64/

# jvm
rm -rf ../src/desktopMain
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
