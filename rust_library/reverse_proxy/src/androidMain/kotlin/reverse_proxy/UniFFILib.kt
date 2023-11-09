package reverse_proxy



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "reverse_proxy"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "reverse_proxy"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("reverse_proxy_2e6f_start")
    actual external fun reverse_proxy_2e6f_start(`proxyPort`: UShort,`frontendPort`: UShort,`frontendCertsPath`: RustBuffer,`frontendKeyPath`: RustBuffer,`backendPort`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("reverse_proxy_2e6f_add")
    actual external fun reverse_proxy_2e6f_add(`a`: Int,`b`: Int,
    _uniffi_out_err: RustCallStatus
    ): Int

    @JvmName("ffi_reverse_proxy_2e6f_rustbuffer_alloc")
    actual external fun ffi_reverse_proxy_2e6f_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_reverse_proxy_2e6f_rustbuffer_from_bytes")
    actual external fun ffi_reverse_proxy_2e6f_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_reverse_proxy_2e6f_rustbuffer_free")
    actual external fun ffi_reverse_proxy_2e6f_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_reverse_proxy_2e6f_rustbuffer_reserve")
    actual external fun ffi_reverse_proxy_2e6f_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}