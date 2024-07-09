echo "cargo building aarch64-pc-windows-gnullvm..."
cargo build --target aarch64-pc-windows-gnullvm --release --quiet

mkdir -p ../src/desktopMain/resources/win32-aarch64/
cp -r ./target/aarch64-pc-windows-gnullvm/release/keychainstore.dll ../src/desktopMain/resources/win32-aarch64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
