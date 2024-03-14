package biometrics



expect object UniFFILib {
    fun biometrics_bc7f_check_support_biometrics(
    _uniffi_out_err: RustCallStatus
    ): Byte

    fun biometrics_bc7f_biometrics_result_content(`reason`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Byte

    fun ffi_biometrics_bc7f_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_biometrics_bc7f_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_biometrics_bc7f_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_biometrics_bc7f_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}