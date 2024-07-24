echo "cargo building aarch64-pc-windows-msvc..."
cargo build --target aarch64-pc-windows-msvc --release --quiet

mkdir -p ../src/desktopMain/resources/win32-aarch64/
cp -r ./target/aarch64-pc-windows-msvc/release/resvg_render.dll ../src/desktopMain/resources/win32-aarch64/

# jvm
cp -r ./target/bindings/jvmMain/ ../src/desktopMain
