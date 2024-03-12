#[cfg(target_os = "macos")]
#[path = "macos.rs"]
mod os;

#[cfg(target_os = "windows")]
#[path = "windows.rs"]
mod os;

pub fn check_support_biometrics() -> i8 {
  os::check_support_biometrics(None)
}

pub fn biometrics_result_content() -> i8 {
  os::biometrics_result_content(None, "test".to_string())
}

uniffi::include_scaffolding!("biometrics");
