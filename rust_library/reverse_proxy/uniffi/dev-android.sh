rm -rf ../src/androidMain

RUSTFLAGS="-C link-args=-Wl,-z,max-page-size=16384" cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

