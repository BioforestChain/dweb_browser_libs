use hardware_info_win::get_hardware_info;

fn main() {
    let winHardwareInfoData = get_hardware_info();
    println!("QAQ winHardwareInfoData = {:?}", winHardwareInfoData);
}
