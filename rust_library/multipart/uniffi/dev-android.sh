rm -rf ../src/androidMain

cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build

# double build，for uniffi_bindgen::generate_external_bindings
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build
cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

