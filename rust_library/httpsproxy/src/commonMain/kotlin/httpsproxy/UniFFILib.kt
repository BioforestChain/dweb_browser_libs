package httpsproxy



expect object UniFFILib {
    fun httpsproxy_26fe_start(`port`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_httpsproxy_26fe_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_httpsproxy_26fe_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_httpsproxy_26fe_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_httpsproxy_26fe_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}