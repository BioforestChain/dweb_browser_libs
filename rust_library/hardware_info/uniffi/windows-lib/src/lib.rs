use serde::{Deserialize, Serialize};
use windows::{
    core::HRESULT,
    Win32::System::Com::{CoInitializeEx, CoUninitialize, COINIT_MULTITHREADED},
};
use wmi::{COMLibrary, Variant, WMIConnection};

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "PascalCase")]
struct Win32_ComputerSystemProduct {
    uuid: String,
}

#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "PascalCase")]
struct Win32_Processor {
    name: String,
    number_of_cores: u32,
    number_of_logical_processors: u32,
    max_clock_speed: u32,
}

#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "PascalCase")]
struct Win32_VideoController {
    name: String,
    adapter_ram: u64,
    driver_version: String,
}

#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "PascalCase")]
struct Win32_DiskDrive {
    model: String,
    size: u64,
}

#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "PascalCase")]
struct Win32_PhysicalMemory {
    capacity: u64,
    speed: u32,
    manufacturer: Option<String>,
}

#[derive(Debug, Default, Clone, Serialize)]
pub struct WinHardwareInfoData {
    pub uuid: String,
    pub cpu_info: Option<String>,
    pub disk_info: Option<String>,
    pub gpu_info: Option<String>,
    pub memory_info: Option<String>,
}

impl WinHardwareInfoData {
    fn new(uuid: String) -> Self {
        Self {
            uuid: uuid,
            cpu_info: None,
            disk_info: None,
            gpu_info: None,
            memory_info: None,
        }
    }

    fn set_cpu_info(&mut self, cpu_info: String) {
        self.cpu_info = Some(cpu_info);
    }

    fn set_disk_info(&mut self, disk_info: String) {
        self.disk_info = Some(disk_info);
    }

    fn set_gpu_info(&mut self, gpu_info: String) {
        self.gpu_info = Some(gpu_info);
    }

    fn set_memory_info(&mut self, memory_info: String) {
        self.memory_info = Some(memory_info);
    }
}

pub fn get_hardware_info() -> WinHardwareInfoData {
    unsafe {
        let _: HRESULT = CoInitializeEx(Some(std::ptr::null_mut()), COINIT_MULTITHREADED);
    }

    let com_con = COMLibrary::new().unwrap();
    let wmi_con = WMIConnection::new(com_con).unwrap();

    let uuid = get_uuid(&wmi_con);
    let mut winHardwareInfoData = WinHardwareInfoData::new(uuid);
    get_cpu_info(&wmi_con, &mut winHardwareInfoData);
    get_disk_info(&wmi_con, &mut winHardwareInfoData);
    get_gpu_info(&wmi_con, &mut winHardwareInfoData);
    get_memory_info(&wmi_con, &mut winHardwareInfoData);

    unsafe { CoUninitialize() }

    winHardwareInfoData
}

fn get_uuid(wmi_con: &WMIConnection) -> String {
    let uuid_results: Vec<Win32_ComputerSystemProduct> = wmi_con.query().unwrap();

    uuid_results.first().unwrap().uuid.clone()
}

fn get_cpu_info(wmi_con: &WMIConnection, winHardwareInfoData: &mut WinHardwareInfoData) {
    let results: Vec<Win32_Processor> = wmi_con.query().unwrap();
    // for cpu in results {
    //     println!("CPU Name: {}", cpu.name);
    //     println!("Number of Cores: {}", cpu.number_of_cores);
    //     println!(
    //         "Number of Logical Processors: {}",
    //         cpu.number_of_logical_processors
    //     );
    //     println!("Max Clock Speed: {} MHz", cpu.max_clock_speed);
    // }
    winHardwareInfoData.set_cpu_info(serde_json::to_string(&results).unwrap());
}

fn get_gpu_info(wmi_con: &WMIConnection, winHardwareInfoData: &mut WinHardwareInfoData) {
    let results: Vec<Win32_VideoController> = wmi_con.query().unwrap();
    // for gpu in results {
    //     println!("GPU Name: {}", gpu.name);
    //     println!("Adapter RAM: {} bytes", gpu.adapter_ram);
    //     println!("Driver Version: {}", gpu.driver_version);
    // }
    winHardwareInfoData.set_gpu_info(serde_json::to_string(&results).unwrap());
}

fn get_disk_info(wmi_con: &WMIConnection, winHardwareInfoData: &mut WinHardwareInfoData) {
    let results: Vec<Win32_DiskDrive> = wmi_con.query().unwrap();
    // for disk in results {
    //     println!("Disk Model: {}", disk.model);
    //     println!("Disk Size: {} bytes", disk.size);
    // }
    winHardwareInfoData.set_disk_info(serde_json::to_string(&results).unwrap());
}

fn get_memory_info(wmi_con: &WMIConnection, winHardwareInfoData: &mut WinHardwareInfoData) {
    let results: Vec<Win32_PhysicalMemory> = wmi_con.query().unwrap();
    // for mem in results {
    //     println!("Memory Capacity: {} bytes", mem.capacity);
    //     println!("Memory Speed: {} MHz", mem.speed);
    //     if let Some(manufacturer) = &mem.manufacturer {
    //         println!("Memory Manufacturer: {}", manufacturer);
    //     }
    // }
    winHardwareInfoData.set_memory_info(serde_json::to_string(&results).unwrap());
}
