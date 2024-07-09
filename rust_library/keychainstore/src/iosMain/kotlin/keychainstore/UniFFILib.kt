package keychainstore



actual object UniFFILib {
    init {
        
    }

    actual fun keychainstore_8e97_get_item(`key`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(keychainstore.cinterop.keychainstore_8e97_get_item(`key`,
    _uniffi_out_err
        ))

    actual fun ffi_keychainstore_8e97_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(keychainstore.cinterop.ffi_keychainstore_8e97_rustbuffer_alloc(`size`,
    _uniffi_out_err
        ))

    actual fun ffi_keychainstore_8e97_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(keychainstore.cinterop.ffi_keychainstore_8e97_rustbuffer_from_bytes(`bytes`,
    _uniffi_out_err
        ))

    actual fun ffi_keychainstore_8e97_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(keychainstore.cinterop.ffi_keychainstore_8e97_rustbuffer_free(`buf`,
    _uniffi_out_err
        ))

    actual fun ffi_keychainstore_8e97_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(keychainstore.cinterop.ffi_keychainstore_8e97_rustbuffer_reserve(`buf`,`additional`,
    _uniffi_out_err
        ))

    
}