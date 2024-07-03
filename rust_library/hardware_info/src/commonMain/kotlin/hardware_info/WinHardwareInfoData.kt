package hardware_info


import okio.Buffer

data class WinHardwareInfoData (
    var `uuid`: String, 
    var `cpuInfo`: String?, 
    var `diskInfo`: String?, 
    var `gpuInfo`: String?, 
    var `memoryInfo`: String?
) {
    
}

object FfiConverterTypeWinHardwareInfoData: FfiConverterRustBuffer<WinHardwareInfoData> {
    override fun read(buf: Buffer): WinHardwareInfoData {
        return WinHardwareInfoData(
            FfiConverterString.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalString.read(buf),
        )
    }

    override fun allocationSize(value: WinHardwareInfoData) = (
            FfiConverterString.allocationSize(value.`uuid`) +
            FfiConverterOptionalString.allocationSize(value.`cpuInfo`) +
            FfiConverterOptionalString.allocationSize(value.`diskInfo`) +
            FfiConverterOptionalString.allocationSize(value.`gpuInfo`) +
            FfiConverterOptionalString.allocationSize(value.`memoryInfo`)
    )

    override fun write(value: WinHardwareInfoData, buf: Buffer) {
            FfiConverterString.write(value.`uuid`, buf)
            FfiConverterOptionalString.write(value.`cpuInfo`, buf)
            FfiConverterOptionalString.write(value.`diskInfo`, buf)
            FfiConverterOptionalString.write(value.`gpuInfo`, buf)
            FfiConverterOptionalString.write(value.`memoryInfo`, buf)
    }
}