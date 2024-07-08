/**
 * 实现主要参考 keyring-rs 项目
 * https://github.com/hwchen/keyring-rs/blob/master/src/windows.rs
 */
pub mod validate;

use std::iter::once;
use std::mem::MaybeUninit;
use validate::{validate_comment, validate_target_alias, validate_target_name, validate_username};
use windows::core::{Error, PCWSTR, PWSTR};
use windows::Win32::Foundation::{
    GetLastError, ERROR_BAD_USERNAME, ERROR_INVALID_FLAGS, ERROR_INVALID_PARAMETER,
    ERROR_NOT_FOUND, ERROR_NO_SUCH_LOGON_SESSION, FILETIME, WIN32_ERROR,
};
use windows::Win32::Security::Credentials::{
    CredDeleteW, CredFree, CredReadA, CredReadW, CredWriteW, CREDENTIALW, CREDENTIAL_ATTRIBUTEW,
    CRED_FLAGS, CRED_MAX_CREDENTIAL_BLOB_SIZE, CRED_MAX_GENERIC_TARGET_NAME_LENGTH,
    CRED_MAX_STRING_LENGTH, CRED_MAX_USERNAME_LENGTH, CRED_PERSIST_ENTERPRISE, CRED_TYPE_GENERIC,
};

/**
 * windows 只对 target_name 做索引
 * 所以在和 apple 兼容时，可以让 apple 中 service + account 的模式，在这里换成 target_name
 * 同时 target_alias 留空, username 仍然使用 account 作为冗余
 * commont
 *
 */

pub fn cred_write(
    target_name: &str,
    password: &mut [u8],
    target_alias: &str,
    username: &str,
    comment: &str,
) -> bool {
    match safe_cred_write(target_name, password, target_alias, username, comment) {
        Ok(_) => true,
        Err(_) => false,
    }
}
fn safe_cred_write(
    target_name: &str,
    password: &mut [u8],
    target_alias: &str,
    username: &str,
    comment: &str,
) -> Result<(), String> {
    validate_username(username)?;
    validate_target_alias(target_alias)?;
    validate_comment(comment)?;
    let mut username = to_wstr(username);
    let mut target_name = to_wstr(target_name);
    let mut target_alias = to_wstr(target_alias);
    let mut comment = to_wstr(comment);

    let flags = CRED_FLAGS::default();
    let cred_type = CRED_TYPE_GENERIC;
    let persist = CRED_PERSIST_ENTERPRISE;
    // Ignored by CredWriteW
    let last_written = FILETIME {
        dwLowDateTime: 0,
        dwHighDateTime: 0,
    };
    let attribute_count = 0;
    let attributes: *mut CREDENTIAL_ATTRIBUTEW = std::ptr::null_mut();
    let mut credential = CREDENTIALW {
        Flags: flags,
        Type: cred_type,
        TargetName: PWSTR(target_name.as_mut_ptr()),
        Comment: PWSTR(comment.as_mut_ptr()),
        LastWritten: last_written,
        CredentialBlobSize: password.len() as u32,
        CredentialBlob: password.as_mut_ptr(),
        Persist: persist,
        AttributeCount: attribute_count,
        Attributes: attributes,
        TargetAlias: PWSTR(target_alias.as_mut_ptr()),
        UserName: PWSTR(username.as_mut_ptr()),
    };
    // raw pointer to credential, is coerced from &mut
    let p_credential: *const CREDENTIALW = &mut credential;
    // Call windows API
    return match unsafe { CredWriteW(p_credential, 0) } {
        Err(_) => Err(decode_last_error()),
        _ => Ok(()),
    };
}

pub fn cred_has(target_name: &str) -> bool {
    // at this point, p_credential is just a pointer to nowhere.
    // The allocation happens in the `CredReadW` call below.
    let mut p_credential = MaybeUninit::uninit();
    let result = {
        let cred_type = CRED_TYPE_GENERIC;
        let target_name = to_wstr(target_name);
        unsafe {
            CredReadW(
                PCWSTR::from_raw(target_name.as_ptr()),
                cred_type,
                0,
                p_credential.as_mut_ptr(),
            )
        }
    };

    return match result {
        // `CredReadW` failed, so no allocation has been done, so no free needs to be done
        Err(_) => false,
        _ => {
            let p_credential = unsafe { p_credential.assume_init() };
            unsafe { CredFree(p_credential as *mut _) };
            true
        }
    };
}
pub fn cred_get(target_name: &str) -> Option<CredentialItem> {
    match validate_target_name(target_name) {
        Err(_) => return None,
        _ => {}
    }
    // at this point, p_credential is just a pointer to nowhere.
    // The allocation happens in the `CredReadW` call below.
    let mut p_credential = MaybeUninit::uninit();
    let result = {
        let cred_type = CRED_TYPE_GENERIC;
        let target_name = to_wstr(target_name);
        unsafe {
            CredReadW(
                PCWSTR::from_raw(target_name.as_ptr()),
                cred_type,
                0,
                p_credential.as_mut_ptr(),
            )
        }
    };

    return match result {
        // `CredReadW` failed, so no allocation has been done, so no free needs to be done
        Err(_) => None,
        _ => {
            // `CredReadW` succeeded, so p_credential points at an allocated credential.
            // To do anything with it, we need to cast it to the right type.  That takes two steps:
            // first we remove the "uninitialized" guard from around it, then we reinterpret it as a
            // pointer to the right structure type.
            let p_credential = unsafe { p_credential.assume_init() };
            let w_credential: CREDENTIALW = unsafe { *p_credential };
            let cred_item = CredentialItem::extract_from_credential(&w_credential);
            unsafe { CredFree(p_credential as *mut _) };
            Some(cred_item)
        }
    };
}
pub fn cred_delete(target_name: &str) -> bool {
    match validate_target_name(target_name) {
        Ok(_) => {}
        Err(_) => return false,
    }
    let target_name = to_wstr(target_name);
    let cred_type = CRED_TYPE_GENERIC;
    match unsafe { CredDeleteW(PCWSTR::from_raw(target_name.as_ptr()), cred_type, 0) } {
        Ok(_) => true,
        Err(_) => false,
    }
}
pub struct CredentialItem {
    pub username: String,
    pub target_name: String,
    pub target_alias: String,
    pub comment: String,
    pub password: Vec<u8>,
}
impl CredentialItem {
    fn extract_from_credential(credential: &CREDENTIALW) -> Self {
        // get password blob
        let password_pointer: *const u8 = credential.CredentialBlob;
        let password_len: usize = credential.CredentialBlobSize as usize;
        let password = unsafe { std::slice::from_raw_parts(password_pointer, password_len) };
        return Self {
            username: unsafe { from_wstr(credential.UserName.as_ptr()) },
            target_name: unsafe { from_wstr(credential.TargetName.as_ptr()) },
            target_alias: unsafe { from_wstr(credential.TargetAlias.as_ptr()) },
            comment: unsafe { from_wstr(credential.Comment.as_ptr()) },
            password: password.into(),
        };
    }
}

unsafe fn from_wstr(ws: *const u16) -> String {
    // null pointer case, return empty string
    if ws.is_null() {
        return String::new();
    }
    // this code from https://stackoverflow.com/a/48587463/558006
    let len = (0..).take_while(|&i| *ws.offset(i) != 0).count();
    if len == 0 {
        return String::new();
    }
    let slice = std::slice::from_raw_parts(ws, len);
    String::from_utf16_lossy(slice)
}

fn to_wstr(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(once(0)).collect()
}
fn to_wstr_no_null(s: &str) -> Vec<u16> {
    s.encode_utf16().collect()
}
/// Map the last encountered Windows API error to a crate error with appropriate annotation.
pub fn decode_last_error() -> String {
    match unsafe { GetLastError() } {
        ERROR_NOT_FOUND => "No matching entry found in keychainstore".to_owned(),
        ERROR_NO_SUCH_LOGON_SESSION => format!(
            "Couldn't access keychainstore, no such logon session: {:?}",
            ERROR_NO_SUCH_LOGON_SESSION
        ),
        err => format!("keychainstore error: {:?}", err),
    }
}

pub fn decode_win32_error(error: WIN32_ERROR) -> String {
    match error {
        ERROR_NOT_FOUND => "No matching entry found in keychainstore".to_owned(),
        ERROR_NO_SUCH_LOGON_SESSION => format!(
            "Couldn't access keychainstore, no such logon session: {:?}",
            ERROR_NO_SUCH_LOGON_SESSION
        ),
        err => format!("keychainstore error: {:?}", err),
    }
}
// pub fn decode_window_error(window_err:Error) -> String {
//     match window_err {
//         ERROR_NOT_FOUND => "No matching entry found in keychainstore".to_owned(),
//         ERROR_NO_SUCH_LOGON_SESSION => {
//             format!("Couldn't access keychainstore: {ERROR_NO_SUCH_LOGON_SESSION}",).to_owned()
//         }
//         err => format!("keychainstore error: {err}").to_owned(),
//     }
// }
