#[cfg(any(target_os = "macos", target_os = "ios"))]
#[path = "apple.rs"]
mod os;

#[cfg(target_os = "windows")]
#[path = "windows.rs"]
mod os;

pub fn get_item(key: String) -> Vec<u8> {
    os::get_item(&key)
}

uniffi::include_scaffolding!("keychainstore");
