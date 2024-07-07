use keychainstore_windows::KeyChainGenericStore;

fn main() {
    let store = KeyChainGenericStore {};
    let target_name = "gaubee";

    assert!(
        store.cred_has(target_name) == false,
        "cred should no has this key"
    );
    assert!(
        store.cred_delete(target_name) == false,
        "cred should no delete this key"
    );

    // let password = "Passw0rdx柯肇丰😊";
    // let password_bytes: &mut [u8] = unsafe {
    //     std::slice::from_raw_parts_mut(password.as_bytes().as_ptr() as *mut u8, password.len())
    // };
    let mut password_vec: Vec<u8> = (0..=255).collect();

    let password_bytes: &mut [u8] = password_vec.as_mut_slice(); // &mut [0,1,2,3,4,5,6,7,8,9,10,11,12];
    let success = store.cred_write(
        target_name,
        password_bytes,
        "some-aliasx",
        "some-userx",
        "some-commentx",
    );
    assert!(success, "cred write fail");

    assert!(store.cred_has(target_name), "cred should has this key");

    match store.cred_get(target_name) {
        Some(item) => {
            println!(
                "cred get password:{:?}",
                // std::str::from_utf8(&item.password).unwrap()
                item.password
            );
            assert!(item.password.eq(password_bytes), "pwd no equals")
        }
        None => assert!(false, "cred should get key success"),
    }

    assert!(store.cred_delete(target_name), "cred delete fail")
}
