package reverse_proxy



expect object UniFFILib {
    fun reverse_proxy_2e6f_start(`proxyPort`: UShort,`frontendPort`: UShort,`frontendCertsPath`: RustBuffer,`frontendKeyPath`: RustBuffer,`backendPort`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun reverse_proxy_2e6f_add(`a`: Int,`b`: Int,
    _uniffi_out_err: RustCallStatus
    ): Int

    fun ffi_reverse_proxy_2e6f_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_reverse_proxy_2e6f_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_reverse_proxy_2e6f_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_reverse_proxy_2e6f_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}