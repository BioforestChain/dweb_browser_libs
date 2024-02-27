# macos
echo "cargo building aarch64-apple-darwin..."
cargo build --target aarch64-apple-darwin --quiet

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
