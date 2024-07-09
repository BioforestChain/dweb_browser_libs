use camino::Utf8Path;
use std::env;
use std::path::Path;
use std::{fs, io::Error, thread, time::Duration};
use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    let path = env::current_dir().unwrap();
    let test = path
        .parent()
        .unwrap()
        .file_name()
        .unwrap()
        .to_str()
        .unwrap()
        .to_string();

    let udl_file_path = format!("./src/{}.udl", test);
    let udl_file = Utf8Path::new(&udl_file_path);
    uniffi::generate_scaffolding(udl_file).unwrap();

    let out_dir = Utf8Path::new("target/bindings");
    let target = env::var("TARGET").unwrap();
    let mode = if cfg!(debug_assertions) {
        "debug"
    } else {
        "release"
    };
    let library_file_path = format!(
        "./target/{}/{}/lib{}.{}",
        target,
        mode,
        test,
        if target.contains("ios") || target.contains("darwin") {
            "a"
        } else if target.contains("windows") {
            "dll"
        } else {
            "so"
        }
    );
    let library_file_path_clone = library_file_path.clone();
    let udl_file = Utf8Path::new(&udl_file_path);
    let library_file = Utf8Path::new(&library_file_path_clone);
    println!(
        "library_file={},exists={}",
        library_file,
        library_file.exists()
    );
    if library_file.exists() {
        uniffi_bindgen::generate_external_bindings(
            KotlinBindingGenerator {},
            udl_file,
            None::<&Utf8Path>,
            Some(out_dir),
            Some(library_file),
            Some(&test),
        )
        .unwrap();
    }
}
