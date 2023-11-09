package reverse_proxy



actual object UniFFILib {
    init {
        
    }

    actual fun reverse_proxy_2e6f_start(`proxyPort`: UShort,`frontendPort`: UShort,`frontendCertsPath`: RustBuffer,`frontendKeyPath`: RustBuffer,`backendPort`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(reverse_proxy.cinterop.reverse_proxy_2e6f_start(`proxyPort`,`frontendPort`,`frontendCertsPath`,`frontendKeyPath`,`backendPort`,
    _uniffi_out_err
        ))

    actual fun reverse_proxy_2e6f_add(`a`: Int,`b`: Int,
    _uniffi_out_err: RustCallStatus
    ): Int =
        requireNotNull(reverse_proxy.cinterop.reverse_proxy_2e6f_add(`a`,`b`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_2e6f_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_2e6f_rustbuffer_alloc(`size`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_2e6f_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_2e6f_rustbuffer_from_bytes(`bytes`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_2e6f_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_2e6f_rustbuffer_free(`buf`,
    _uniffi_out_err
        ))

    actual fun ffi_reverse_proxy_2e6f_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(reverse_proxy.cinterop.ffi_reverse_proxy_2e6f_rustbuffer_reserve(`buf`,`additional`,
    _uniffi_out_err
        ))

    
}