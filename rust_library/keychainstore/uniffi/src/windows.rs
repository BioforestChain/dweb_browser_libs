pub fn get_item(scope: &str, key: &str) -> Option<Vec<u8>> {
    return keychainstore_windows::cred_get(&format!("{scope}/{key}")).map(|x|{x.password});
}
pub fn set_item(scope: &str, key: &str, value: &[u8]) -> bool {
    return keychainstore_windows::cred_write(
        &format!("{scope}/{key}"),
        &mut value.to_vec().clone(),
        "",
        key,
        "dweb keychainstore",
    );
}
pub fn has_item(scope: &str, key: &str) -> bool {
    return keychainstore_windows::cred_has(&format!("{scope}/{key}"));
}
pub fn delete_item(scope: &str, key: &str) -> bool {
    return keychainstore_windows::cred_delete(&format!("{scope}/{key}"));
}
pub fn support_enum_keys() -> bool {
    return false;
}
pub fn item_keys(scope: &str) -> Vec<String> {
    return vec![];
}
