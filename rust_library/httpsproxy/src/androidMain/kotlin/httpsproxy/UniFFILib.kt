package httpsproxy



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "httpsproxy"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "httpsproxy"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("httpsproxy_26fe_start")
    actual external fun httpsproxy_26fe_start(`port`: UShort,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_httpsproxy_26fe_rustbuffer_alloc")
    actual external fun ffi_httpsproxy_26fe_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_httpsproxy_26fe_rustbuffer_from_bytes")
    actual external fun ffi_httpsproxy_26fe_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_httpsproxy_26fe_rustbuffer_free")
    actual external fun ffi_httpsproxy_26fe_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_httpsproxy_26fe_rustbuffer_reserve")
    actual external fun ffi_httpsproxy_26fe_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}