pub fn get_item(key: &str) -> Options<Vec<u8>> {
    keychainstore_windows::get_item()
}
