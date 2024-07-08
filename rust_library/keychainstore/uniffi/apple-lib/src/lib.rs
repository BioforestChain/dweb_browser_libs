use swift_rs::{swift, Bool, SRArray, SRData, SRObject, SRString};

swift!(pub(crate) fn store_save_item(service: &SRString, account: &SRString, data: SRData));
swift!(pub(crate) fn store_has_item(service: &SRString, account: &SRString) -> Bool);
swift!(pub(crate) fn store_load_item(service: &SRString, account: &SRString) -> Option<SRData>);
swift!(pub(crate) fn store_delete_item(service: &SRString, account: &SRString) -> Bool);
#[repr(C)]
pub(crate) struct SwiftAccountArray {
    data: SRArray<SRString>,
}
swift!(pub(crate) fn store_get_all_accounts(service: &SRString) -> SRObject<SwiftAccountArray>);

pub fn save_item(service: &str, account: &str, data: &[u8]) {
    return unsafe { store_save_item(&service.into(), &account.into(), data.into()) };
}
pub fn has_item(service: &str, account: &str) -> Bool {
    return unsafe { store_has_item(&service.into(), &account.into()) };
}
pub fn load_item(service: &str, account: &str) -> Option<Vec<u8>> {
    return unsafe { store_load_item(&service.into(), &account.into()).map(|d| d.to_vec()) };
}
pub fn delete_item(service: &str, account: &str) -> Bool {
    return unsafe { store_delete_item(&service.into(), &account.into()) };
}
pub fn get_all_accounts(service: &str) -> Vec<String> {
    return unsafe {
        store_get_all_accounts(&service.into())
            .data
            .as_slice()
            .into_iter()
            .map(|s| s.to_string())
            .collect()
    };
}
