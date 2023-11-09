use camino::Utf8Path;
use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    let out_dir = Utf8Path::new("target/bindings");
    let udl_file_path = Utf8Path::new("./src/reverse_proxy.udl");
    let udl_file = Utf8Path::new(&udl_file_path);
    uniffi::generate_scaffolding(udl_file).unwrap();
    uniffi_bindgen::generate_external_bindings(
        KotlinBindingGenerator {},
        udl_file,
        None::<&Utf8Path>,
        Some(out_dir),
    )
    .unwrap();
}
