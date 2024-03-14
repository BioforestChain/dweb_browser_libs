package biometrics



expect object UniFFILib {
    fun biometrics_5a15_check_support_biometrics(
    _uniffi_out_err: RustCallStatus
    ): Byte

    fun biometrics_5a15_biometrics_result_content(`reason`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_biometrics_5a15_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_biometrics_5a15_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_biometrics_5a15_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_biometrics_5a15_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}