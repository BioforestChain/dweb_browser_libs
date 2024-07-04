package hardware_info



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "hardware_info"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "hardware_info"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("hardware_info_bed_get_hardware_info")
    actual external fun hardware_info_bed_get_hardware_info(
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_hardware_info_bed_rustbuffer_alloc")
    actual external fun ffi_hardware_info_bed_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_hardware_info_bed_rustbuffer_from_bytes")
    actual external fun ffi_hardware_info_bed_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_hardware_info_bed_rustbuffer_free")
    actual external fun ffi_hardware_info_bed_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_hardware_info_bed_rustbuffer_reserve")
    actual external fun ffi_hardware_info_bed_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}