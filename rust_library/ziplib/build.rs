use std::env;
use std::path::PathBuf;

fn main() {
    // The ANDROID_NDK_ROOT variable is automatically set to <SDK root>/ndk/<NDK version> by the
    // Cargo Gradle plugin. You may want to implement your own logic finding the path to NDK if
    // you are not invoking Cargo from Gradle.
    let android_ndk_root = env::var("ANDROID_NDK_HOME").unwrap();

    // set this to false if you want to use libc++_static.a.
    let use_shared = true;
    let enable_16kb_page_size = true;

    let host = if cfg!(target_os = "windows") {
        "windows-x86_64"
    } else if cfg!(target_os = "macos") {
        // Apple Sillion Macs also use x86_64.
        "darwin-x86_64"
    } else if cfg!(target_os = "linux") {
        "linux-x86_64"
    } else {
        panic!("unsupported host")
    };

    let ndk_triplet = match env::var("CARGO_CFG_TARGET_ARCH").unwrap().as_str() {
        "aarch64" => "aarch64-linux-android",
        "arm" => "arm-linux-androideabi",
        "x86_64" => "x86_64-linux-android",
        "x86" => "i686-linux-android",
        /* RISC-V is not supported by this project yet */
        _ => panic!("unsupported architecture"),
    };

    // `libc++_shared.so` and `libc++_static.a` are in
    // toolchains/llvm/prebuilt/<host>/sysroot/usr/lib/<NDK triplet>.
    _ = PathBuf::from(android_ndk_root)
        .join("toolchains")
        .join("llvm")
        .join("prebuilt")
        .join(host)
        .join("sysroot")
        .join("usr")
        .join("lib")
        .join(ndk_triplet);

    let target_os = env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if enable_16kb_page_size && target_os == "android" {
        println!("Enabling 16KB page size...");
        println!("cargo:rustc-link-arg=-Wl,-z,max-page-size=16384");
    }

    uniffi::generate_scaffolding("uniffi/ziplib.udl").unwrap();
}