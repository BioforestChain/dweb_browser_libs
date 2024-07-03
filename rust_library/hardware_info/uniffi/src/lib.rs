#[cfg(target_os = "windows")]
#[path = "windows.rs"]
mod os;

use os::WinHardwareInfoData;

pub fn get_hardware_info() -> WinHardwareInfoData {
  os::get_hardware_info()
}

uniffi::include_scaffolding!("hardware_info");
