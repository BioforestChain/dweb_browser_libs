use swift_rs::{swift, Bool, SRArray, SRData, SRObject, SRString};

#[repr(C)]
pub(crate) struct SwiftKeyChainGenericStore {}

swift!(pub(crate) fn create_key_chain_generic_store(service: &SRString) -> SRObject<SwiftKeyChainGenericStore>);

swift!(pub(crate) fn store_save_item(store:&SRObject<SwiftKeyChainGenericStore>, account: &SRString, data: SRData));
swift!(pub(crate) fn store_has_item(store:&SRObject<SwiftKeyChainGenericStore>, account: &SRString) -> Bool);
swift!(pub(crate) fn store_load_item(store:&SRObject<SwiftKeyChainGenericStore>, account: &SRString) -> Option<SRData>);
swift!(pub(crate) fn store_delete_item(store:&SRObject<SwiftKeyChainGenericStore>, account: &SRString) -> Bool);

#[repr(C)]
pub(crate) struct SwiftAccountArray {
    data: SRArray<SRString>,
}
swift!(pub(crate) fn store_get_all_accounts(store:&SRObject<SwiftKeyChainGenericStore>) -> SRObject<SwiftAccountArray>);

pub struct KeyChainGenericStore {
    ns_obj: SRObject<SwiftKeyChainGenericStore>,
}

impl KeyChainGenericStore {
    pub fn new(service: &str) -> Self {
        let store = unsafe { create_key_chain_generic_store(&service.into()) };
        return Self { ns_obj: store };
    }
    pub fn save_item(&self, account: &str, data: &[u8]) {
        return unsafe { store_save_item(&self.ns_obj, &account.into(), data.into()) };
    }
    pub fn has_item(&self, account: &str) -> Bool {
        return unsafe { store_has_item(&self.ns_obj, &account.into()) };
    }
    pub fn load_item(&self, account: &str) -> Option<Vec<u8>> {
        return unsafe { store_load_item(&self.ns_obj, &account.into()).map(|d| d.to_vec()) };
    }
    pub fn delete_item(&self, account: &str) -> Bool {
        return unsafe { store_delete_item(&self.ns_obj, &account.into()) };
    }
    pub fn get_all_accounts(&self) -> Vec<String> {
        return unsafe {
            store_get_all_accounts(&self.ns_obj)
                .data
                .as_slice()
                .into_iter()
                .map(|s| s.to_string())
                .collect()
        };
    }
}
