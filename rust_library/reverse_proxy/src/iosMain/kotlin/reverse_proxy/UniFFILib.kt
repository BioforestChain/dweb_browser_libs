package reverse_proxy



actual object UniFFILib {
    init {
        FfiConverterTypeVoidCallback.register(this)
        
    }

    actual fun ffi_reverse_proxy_6e76_VoidCallback_init_callback(`callbackStub`: ForeignCallback,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_6e76_VoidCallback_init_callback(`callbackStub`,
    _uniffi_out_err
        ))

    actual fun reverse_proxy_6e76_start(`backendPort`: UShort,`onReady`: ULong,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(reverse_proxy.cinterop.reverse_proxy_6e76_start(`backendPort`,`onReady`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_6e76_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_6e76_rustbuffer_alloc(`size`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_6e76_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_6e76_rustbuffer_from_bytes(`bytes`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_6e76_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_6e76_rustbuffer_free(`buf`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_6e76_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_6e76_rustbuffer_reserve(`buf`,`additional`,
    _uniffi_out_err
        ))

    
}