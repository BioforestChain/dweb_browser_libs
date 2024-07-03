package hardware_info



fun `getHardwareInfo`(): WinHardwareInfoData {
    return FfiConverterTypeWinHardwareInfoData.lift(
    rustCall() { _status ->
    UniFFILib.hardware_info_bed_get_hardware_info( _status)
})
}

