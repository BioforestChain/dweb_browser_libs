package biometrics



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "biometrics"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "biometrics"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("biometrics_5a15_check_support_biometrics")
    actual external fun biometrics_5a15_check_support_biometrics(
    _uniffi_out_err: RustCallStatus
    ): Byte

    @JvmName("biometrics_5a15_biometrics_result_content")
    actual external fun biometrics_5a15_biometrics_result_content(`reason`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_biometrics_5a15_rustbuffer_alloc")
    actual external fun ffi_biometrics_5a15_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_biometrics_5a15_rustbuffer_from_bytes")
    actual external fun ffi_biometrics_5a15_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_biometrics_5a15_rustbuffer_free")
    actual external fun ffi_biometrics_5a15_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_biometrics_5a15_rustbuffer_reserve")
    actual external fun ffi_biometrics_5a15_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}