pub fn get_item(scope: &str, key: &str) -> Option<Vec<u8>> {
    return keychainstore_apple::load_item(scope, key);
}
pub fn set_item(scope: &str, key: &str, value: &[u8]) -> bool {
    keychainstore_apple::save_item(scope, key, value);
    return true;
}
pub fn has_item(scope: &str, key: &str) -> bool {
    return keychainstore_apple::has_item(scope, key);
}
pub fn delete_item(scope: &str, key: &str) -> bool {
    return keychainstore_apple::delete_item(scope, key);
}
pub fn support_enum_keys() -> bool {
    return true;
}
pub fn item_keys(scope: &str) -> Vec<String> {
    return keychainstore_apple::get_all_accounts(scope);
}
