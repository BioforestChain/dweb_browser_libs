# android
rm -rf ../src/androidMain

echo "cargo building aarch64-linux-android..."
RUSTFLAGS="-C link-args=-Wl,-z,max-page-size=16384" cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release

# double build，for uniffi_bindgen::generate_external_bindings
RUSTFLAGS="-C link-args=-Wl,-z,max-page-size=16384" cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release --quiet

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain
