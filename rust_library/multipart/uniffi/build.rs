use camino::Utf8Path;
use std::env;
use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    let path = env::current_dir().unwrap();
    let test_directory = path.parent().unwrap();
    let test_directory_name = test_directory.file_name().unwrap();
    let test = test_directory_name.to_str().unwrap();

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
        if target.contains("ios") { "a" } else { "so" }
    );
    let library_file = Utf8Path::new(&library_file_path);
    uniffi_bindgen::generate_external_bindings(
        KotlinBindingGenerator {},
        udl_file,
        None::<&Utf8Path>,
        Some(out_dir),
        Some(library_file),
        Some(test),
    )
    .unwrap();
}
