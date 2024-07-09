package keychainstore



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "keychainstore"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "keychainstore"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("keychainstore_8e97_get_item")
    actual external fun keychainstore_8e97_get_item(`key`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_keychainstore_8e97_rustbuffer_alloc")
    actual external fun ffi_keychainstore_8e97_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_keychainstore_8e97_rustbuffer_from_bytes")
    actual external fun ffi_keychainstore_8e97_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_keychainstore_8e97_rustbuffer_free")
    actual external fun ffi_keychainstore_8e97_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_keychainstore_8e97_rustbuffer_reserve")
    actual external fun ffi_keychainstore_8e97_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}