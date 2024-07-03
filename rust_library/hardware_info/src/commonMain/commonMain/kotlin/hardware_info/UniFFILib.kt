package hardware_info



expect object UniFFILib {
    fun hardware_info_bed_get_hardware_info(
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_hardware_info_bed_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_hardware_info_bed_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_hardware_info_bed_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_hardware_info_bed_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}