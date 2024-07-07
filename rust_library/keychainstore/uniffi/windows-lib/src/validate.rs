use windows::Win32::Security::Credentials::{
    CRED_MAX_CREDENTIAL_BLOB_SIZE, CRED_MAX_GENERIC_TARGET_NAME_LENGTH, CRED_MAX_STRING_LENGTH,
    CRED_MAX_USERNAME_LENGTH,
};

fn error_too_long(name: &str, len: u32) -> String {
    format!("Attribute '{name}' is longer than platform limit of {len} chars")
}
fn error_invalid(attr: &str, reason: &str) -> String {
    format!("Attribute {attr} is invalid: {reason}")
}

pub fn validate_username(username: &str) -> Result<(), String> {
    if username.len() > CRED_MAX_USERNAME_LENGTH as usize {
        return Result::Err(error_too_long("username", CRED_MAX_USERNAME_LENGTH));
    }
    Ok(())
}

pub fn validate_target_name(target_name: &str) -> Result<(), String> {
    if target_name.is_empty() {
        return Result::Err(error_invalid("target", "cannot be empty"));
    }
    if target_name.len() > CRED_MAX_GENERIC_TARGET_NAME_LENGTH as usize {
        return Result::Err(error_too_long(
            "target",
            CRED_MAX_GENERIC_TARGET_NAME_LENGTH,
        ));
    }
    Ok(())
}

pub fn validate_target_alias(target_alias: &str) -> Result<(), String> {
    if target_alias.len() > CRED_MAX_STRING_LENGTH as usize {
        return Result::Err(error_too_long("target alias", CRED_MAX_STRING_LENGTH));
    }
    Ok(())
}

pub fn validate_comment(comment: &str) -> Result<(), String> {
    if comment.len() > CRED_MAX_STRING_LENGTH as usize {
        return Result::Err(error_too_long("comment", CRED_MAX_STRING_LENGTH));
    }
    Ok(())
}

pub fn validate_password(password: &str) -> Result<(), String> {
    // We're going to store the password as UTF-16, so make sure to consider its length as UTF-16.
    // `encode_utf16` gives us the count of `u16`s, so we multiply by 2 to get the number of bytes.
    if password.encode_utf16().count() * 2 > CRED_MAX_CREDENTIAL_BLOB_SIZE as usize {
        return Result::Err(error_too_long("password", CRED_MAX_CREDENTIAL_BLOB_SIZE));
    }
    Ok(())
}
