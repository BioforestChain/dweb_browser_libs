package httpsproxy



actual object UniFFILib {
    init {
        
    }

    actual fun httpsproxy_26fe_start(`port`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(httpsproxy.cinterop.httpsproxy_26fe_start(`port`,
    _uniffi_out_err
        ))

    actual fun ffi_httpsproxy_26fe_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(httpsproxy.cinterop.ffi_httpsproxy_26fe_rustbuffer_alloc(`size`,
    _uniffi_out_err
        ))

    actual fun ffi_httpsproxy_26fe_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(httpsproxy.cinterop.ffi_httpsproxy_26fe_rustbuffer_from_bytes(`bytes`,
    _uniffi_out_err
        ))

    actual fun ffi_httpsproxy_26fe_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(httpsproxy.cinterop.ffi_httpsproxy_26fe_rustbuffer_free(`buf`,
    _uniffi_out_err
        ))

    actual fun ffi_httpsproxy_26fe_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(httpsproxy.cinterop.ffi_httpsproxy_26fe_rustbuffer_reserve(`buf`,`additional`,
    _uniffi_out_err
        ))

    
}