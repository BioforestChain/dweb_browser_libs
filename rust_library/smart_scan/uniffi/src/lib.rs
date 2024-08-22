#[cfg(target_os = "macos")]
#[path = "macos.rs"]
mod os;

#[cfg(target_os = "windows")]
#[path = "windows.rs"]
mod os;

mod consts;
use consts::BiometricsResult;

pub fn check_support_biometrics() -> i8 {
  os::check_support_biometrics()
}

pub fn biometrics_result_content(reason: String) -> BiometricsResult {
  let (success, message) = os::biometrics_result_content(reason);
  BiometricsResult {
    success,
    message
  }
}

uniffi::include_scaffolding!("biometrics");
