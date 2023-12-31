package reverse_proxy

import kotlinx.cinterop.*
import okio.Buffer

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBuffer = CValue<reverse_proxy.cinterop.RustBuffer>
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBufferPointer = CPointer<reverse_proxy.cinterop.RustBuffer>

actual fun RustBuffer.toBuffer(): Buffer {
    val data = useContents { data }
    val len = useContents { len }
    return Buffer().write(requireNotNull(data).readBytes(len))
}

actual val RustBuffer.dataSize: Int
    get() = useContents { len }

actual fun RustBuffer.free(): Unit =
    rustCall { status ->
        UniFFILib.ffi_reverse_proxy_6e76_rustbuffer_free(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status ->
        val size = buffer.size
        UniFFILib.ffi_reverse_proxy_6e76_rustbuffer_alloc(size.toInt(), status).also {
            it.useContents {
                val notNullData = data
                checkNotNull(notNullData) { "RustBuffer.alloc() returned null data pointer (size=${size})" }
                buffer.readByteArray().forEachIndexed { index, byte ->
                    notNullData[index] = byte.toUByte()
                }
            }
        }
    }

actual fun RustBufferPointer.setValue(value: RustBuffer) {
    this.pointed.capacity = value.useContents { capacity }
    this.pointed.len = value.useContents { len }
    this.pointed.data = value.useContents { data }
}

actual fun emptyRustBuffer(): RustBuffer {
    return allocRustBuffer(Buffer())
}