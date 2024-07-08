rm -rf ../src/desktopMain

echo "cargo building x86_64-pc-windows-gnu..."
cargo build --target x86_64-pc-windows-gnu

mkdir -p ../src/desktopMain/resources/win32-x86-64/
cp -r ./target/x86_64-pc-windows-gnu/release/keychainstore.dll ../src/desktopMain/resources/win32-x86-64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
