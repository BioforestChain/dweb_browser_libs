rm -rf ../src/androidMain

cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release

cp -r ./target/bindings/jvmMain/ ../src/androidMain

rm -rf ../src/commonMain
cp -r ./target/bindings/commonMain/ ../src/commonMain

cargo build --target aarch64-apple-ios --release

rm -rf ../src/nativeInterop
cp -r ./target/bindings/nativeInterop/ ../src/nativeInterop
cp ../reverse_proxy.def ../src/nativeInterop/cinterop/

rm -rf ../src/iosMain
cp -r ./target/bindings/nativeMain/ ../src/iosMain

mkdir -p ../src/libs/iosArm64/
cp ./target/aarch64-apple-ios/release/libreverse_proxy.a ../src/libs/iosArm64/
