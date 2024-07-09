#[cfg(any(target_os = "macos", target_os = "ios"))]
#[path = "apple.rs"]
mod os;

#[cfg(target_os = "windows")]
#[path = "windows.rs"]
mod os;

pub fn keychain_get_item(scope: String, key: String) -> Option<Vec<u8>> {
    return os::get_item(&scope, &key);
}
pub fn keychain_set_item(scope: String, key: String, value: Vec<u8>) -> bool {
    return os::set_item(&scope, &key, &value);
}
pub fn keychain_has_item(scope: String, key: String) -> bool {
    return os::has_item(&scope, &key);
}
pub fn keychain_delete_item(scope: String, key: String) -> bool {
    return os::delete_item(&scope, &key);
}
pub fn keychain_support_enum_keys() -> bool {
    return os::support_enum_keys();
}
pub fn keychain_item_keys(scope: String) -> Vec<String> {
    return os::item_keys(&scope);
}

uniffi::include_scaffolding!("keychainstore");
