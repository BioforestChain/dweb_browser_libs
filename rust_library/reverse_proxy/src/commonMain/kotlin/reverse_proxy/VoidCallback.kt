package reverse_proxy



interface VoidCallback {
    fun `callback`(`proxyPort`: UShort, `frontendPort`: UShort)
    
}

object ForeignCallbackTypeVoidCallback {
    @Suppress("TooGenericExceptionCaught")
    fun invoke(handle: Handle, method: Int, args: RustBuffer, outBuf: RustBufferPointer): Int {
        val cb = FfiConverterTypeVoidCallback.lift(handle)
        return when (method) {
            IDX_CALLBACK_FREE -> {
                FfiConverterTypeVoidCallback.drop(handle)
                0
            }
            1 -> {
                try {
                    val buffer = this.`invokeCallback`(cb, args)
                    // Success
                    outBuf.setValue(buffer)
                    1
                } catch (e: Throwable) {
                    try {
                        outBuf.setValue(FfiConverterString.lower(e.toString()))
                    } catch (e: Throwable) {
                    }
                    -1
                }
            }
            
            else -> {
                try {
                    outBuf.setValue(FfiConverterString.lower("Invalid Callback index"))
                } catch (e: Throwable) {
                }
                -1
            }
        }
    }

    
    private fun `invokeCallback`(kotlinCallbackInterface: VoidCallback, args: RustBuffer): RustBuffer =
        try {
            val buf = checkNotNull(args.toBuffer()) { "No Buffer in RustBuffer; this is a Uniffi bug" }
            kotlinCallbackInterface.`callback`(
                    FfiConverterUShort.read(buf), 
                    FfiConverterUShort.read(buf)
                    )
            .let { emptyRustBuffer() }
                } finally {
            args.free()
        }

    
}

object FfiConverterTypeVoidCallback: FfiConverterCallbackInterface<VoidCallback>() {
    override fun register(lib: UniFFILib) {
        rustCall() { status ->
            lib.ffi_reverse_proxy_54a6_VoidCallback_init_callback(ForeignCallbackTypeVoidCallback.toForeignCallback(), status)
        }
    }
}

expect fun ForeignCallbackTypeVoidCallback.toForeignCallback() : ForeignCallback