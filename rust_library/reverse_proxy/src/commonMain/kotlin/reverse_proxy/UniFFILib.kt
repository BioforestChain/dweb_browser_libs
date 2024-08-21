package reverse_proxy



expect object UniFFILib {
    fun ffi_reverse_proxy_60a8_VoidCallback_init_callback(`callbackStub`: ForeignCallback,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun reverse_proxy_60a8_start(`backendPort`: UShort,`onReady`: ULong,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun reverse_proxy_60a8_forward(`newForwardPort`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_reverse_proxy_60a8_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_reverse_proxy_60a8_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_reverse_proxy_60a8_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_reverse_proxy_60a8_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}