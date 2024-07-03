pub use hardware_info_win::WinHardwareInfoData;

pub fn get_hardware_info() -> WinHardwareInfoData {
    hardware_info_win::get_hardware_info()
}
