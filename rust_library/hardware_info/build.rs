fn main() {
    // set this to false if you want to use libc++_static.a.
    let use_shared = true;

    uniffi::generate_scaffolding("uniffi/hardware_info.udl").unwrap();
}