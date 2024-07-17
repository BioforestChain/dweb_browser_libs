// This file was autogenerated by some hot garbage in the `uniffi` crate.
// Trust me, you don't want to mess with it!

@file:Suppress("NAME_SHADOWING")

package biometrics

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Callback
import com.sun.jna.Structure
import com.sun.jna.Structure.ByValue
import com.sun.jna.ptr.ByReference
import java.util.concurrent.ConcurrentHashMap
import okio.Buffer

actual typealias Pointer = com.sun.jna.Pointer

actual fun kotlin.Long.toPointer() = com.sun.jna.Pointer(this)

actual fun Pointer.toLong(): kotlin.Long = com.sun.jna.Pointer.nativeValue(this)

actual typealias UBytePointer = com.sun.jna.Pointer

actual fun UBytePointer.asSource(len: kotlin.Long): NoCopySource = object : NoCopySource {
    val buffer = getByteBuffer(0, len).also {
        it.order(java.nio.ByteOrder.BIG_ENDIAN)
    }

    override fun exhausted(): kotlin.Boolean = !buffer.hasRemaining()

    override fun readByte(): kotlin.Byte = buffer.get()

    override fun readInt(): kotlin.Int = buffer.getInt()

    override fun readLong(): kotlin.Long = buffer.getLong()

    override fun readShort(): kotlin.Short = buffer.getShort()

    override fun readByteArray(): ByteArray {
        val remaining = buffer.remaining()
        return readByteArray(remaining.toLong())
    }

    override fun readByteArray(len: kotlin.Long): ByteArray {
        val startIndex = buffer.position().toLong()
        val indexAfterLast = (startIndex + len).toInt()
        val byteArray = getByteArray(startIndex, len.toInt())
        buffer.position(indexAfterLast)
        return byteArray
    }
}

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStructure : Structure() {
    @JvmField
    var capacity: kotlin.Int = 0

    @JvmField
    var len: kotlin.Int = 0

    @JvmField
    var data: Pointer? = null
}

actual class RustBuffer : RustBufferStructure(), Structure.ByValue

actual class RustBufferPointer : ByReference(16) {
    fun setValueInternal(value: RustBuffer) {
        pointer.setInt(0, value.capacity)
        pointer.setInt(4, value.len)
        pointer.setPointer(8, value.data)
    }
}

actual fun RustBuffer.asSource(): NoCopySource = requireNotNull(data).asSource(len.toLong())

actual val RustBuffer.dataSize: kotlin.Int
    get() = len

actual fun RustBuffer.free() =
    rustCall { status: RustCallStatus ->
        UniFFILib.ffi_biometrics_rustbuffer_free(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status: RustCallStatus ->
        val size = buffer.size
        var readPosition = 0L
        UniFFILib.ffi_biometrics_rustbuffer_alloc(size.toInt(), status).also { rustBuffer: RustBuffer ->
            val data = rustBuffer.data
                ?: throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
            rustBuffer.writeField("len", size.toInt())
            // Loop until the buffer is completed read, okio reads max 8192 bytes
            while (readPosition < size) {
                readPosition += buffer.read(data.getByteBuffer(readPosition, size - readPosition))
            }
        }
    }

actual fun RustBufferPointer.setValue(value: RustBuffer) = setValueInternal(value)

actual fun emptyRustBuffer(): RustBuffer = RustBuffer()

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
actual open class ForeignBytes : Structure() {
    @JvmField
    var len: kotlin.Int = 0

    @JvmField
    var data: Pointer? = null
}
@Structure.FieldOrder("code", "error_buf")
actual open class RustCallStatus : Structure() {
    @JvmField
    var code: kotlin.Byte = 0

    @JvmField
    var error_buf: RustBuffer = RustBuffer()
}

actual val RustCallStatus.statusCode: kotlin.Byte
    get() = code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = error_buf

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T {
    val rustCallStatus = RustCallStatus()
    return block(rustCallStatus)
}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
actual open class RustCallStatusByValue : RustCallStatus(), ByValue

actual class UniFfiHandleMap<T : Any> {
    private val map = ConcurrentHashMap<kotlin.ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter = java.util.concurrent.atomic.AtomicInteger(0)

    actual val size: kotlin.Int
        get() = map.size

    actual fun insert(obj: T): kotlin.ULong {
        val handle = counter.getAndAdd(1).toULong()
        map.put(handle, obj)
        return handle
    }

    actual fun get(handle: kotlin.ULong): T? {
        return map.get(handle)
    }

    actual fun remove(handle: kotlin.ULong): T? {
        return map.remove(handle)
    }
}

// FFI type for Rust future continuations

internal class UniFfiRustFutureContinuationCallbackImpl() : Callback {
    fun invoke(continuationHandle: kotlin.ULong, pollResult: kotlin.Short) = resumeContinutation(continuationHandle, pollResult)
}

internal actual typealias UniFfiRustFutureContinuationCallbackType = UniFfiRustFutureContinuationCallbackImpl

internal actual fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType =
    UniFfiRustFutureContinuationCallbackImpl()

// Contains loading, initialization code,
// and the FFI Function declarations.
@Synchronized
private fun findLibraryName(): kotlin.String {
    val componentName = "biometrics"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "biometrics"
}

actual internal object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("uniffi_biometrics_fn_func_biometrics_result_content")
    actual external fun uniffi_biometrics_fn_func_biometrics_result_content(`reason`: RustBuffer,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer

    @JvmName("uniffi_biometrics_fn_func_check_support_biometrics")
    actual external fun uniffi_biometrics_fn_func_check_support_biometrics(_uniffi_out_err: RustCallStatus, 
    ): Byte

    @JvmName("ffi_biometrics_rustbuffer_alloc")
    actual external fun ffi_biometrics_rustbuffer_alloc(`size`: Int,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer

    @JvmName("ffi_biometrics_rustbuffer_from_bytes")
    actual external fun ffi_biometrics_rustbuffer_from_bytes(`bytes`: ForeignBytes,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer

    @JvmName("ffi_biometrics_rustbuffer_free")
    actual external fun ffi_biometrics_rustbuffer_free(`buf`: RustBuffer,_uniffi_out_err: RustCallStatus, 
    ): Unit

    @JvmName("ffi_biometrics_rustbuffer_reserve")
    actual external fun ffi_biometrics_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer

    @JvmName("ffi_biometrics_rust_future_continuation_callback_set")
    actual external fun ffi_biometrics_rust_future_continuation_callback_set(`callback`: UniFfiRustFutureContinuationCallbackType,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_poll_u8")
    actual external fun ffi_biometrics_rust_future_poll_u8(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_u8")
    actual external fun ffi_biometrics_rust_future_cancel_u8(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_u8")
    actual external fun ffi_biometrics_rust_future_free_u8(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_u8")
    actual external fun ffi_biometrics_rust_future_complete_u8(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UByte

    @JvmName("ffi_biometrics_rust_future_poll_i8")
    actual external fun ffi_biometrics_rust_future_poll_i8(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_i8")
    actual external fun ffi_biometrics_rust_future_cancel_i8(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_i8")
    actual external fun ffi_biometrics_rust_future_free_i8(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_i8")
    actual external fun ffi_biometrics_rust_future_complete_i8(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Byte

    @JvmName("ffi_biometrics_rust_future_poll_u16")
    actual external fun ffi_biometrics_rust_future_poll_u16(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_u16")
    actual external fun ffi_biometrics_rust_future_cancel_u16(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_u16")
    actual external fun ffi_biometrics_rust_future_free_u16(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_u16")
    actual external fun ffi_biometrics_rust_future_complete_u16(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UShort

    @JvmName("ffi_biometrics_rust_future_poll_i16")
    actual external fun ffi_biometrics_rust_future_poll_i16(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_i16")
    actual external fun ffi_biometrics_rust_future_cancel_i16(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_i16")
    actual external fun ffi_biometrics_rust_future_free_i16(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_i16")
    actual external fun ffi_biometrics_rust_future_complete_i16(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Short

    @JvmName("ffi_biometrics_rust_future_poll_u32")
    actual external fun ffi_biometrics_rust_future_poll_u32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_u32")
    actual external fun ffi_biometrics_rust_future_cancel_u32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_u32")
    actual external fun ffi_biometrics_rust_future_free_u32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_u32")
    actual external fun ffi_biometrics_rust_future_complete_u32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UInt

    @JvmName("ffi_biometrics_rust_future_poll_i32")
    actual external fun ffi_biometrics_rust_future_poll_i32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_i32")
    actual external fun ffi_biometrics_rust_future_cancel_i32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_i32")
    actual external fun ffi_biometrics_rust_future_free_i32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_i32")
    actual external fun ffi_biometrics_rust_future_complete_i32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Int

    @JvmName("ffi_biometrics_rust_future_poll_u64")
    actual external fun ffi_biometrics_rust_future_poll_u64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_u64")
    actual external fun ffi_biometrics_rust_future_cancel_u64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_u64")
    actual external fun ffi_biometrics_rust_future_free_u64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_u64")
    actual external fun ffi_biometrics_rust_future_complete_u64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): ULong

    @JvmName("ffi_biometrics_rust_future_poll_i64")
    actual external fun ffi_biometrics_rust_future_poll_i64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_i64")
    actual external fun ffi_biometrics_rust_future_cancel_i64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_i64")
    actual external fun ffi_biometrics_rust_future_free_i64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_i64")
    actual external fun ffi_biometrics_rust_future_complete_i64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Long

    @JvmName("ffi_biometrics_rust_future_poll_f32")
    actual external fun ffi_biometrics_rust_future_poll_f32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_f32")
    actual external fun ffi_biometrics_rust_future_cancel_f32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_f32")
    actual external fun ffi_biometrics_rust_future_free_f32(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_f32")
    actual external fun ffi_biometrics_rust_future_complete_f32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Float

    @JvmName("ffi_biometrics_rust_future_poll_f64")
    actual external fun ffi_biometrics_rust_future_poll_f64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_f64")
    actual external fun ffi_biometrics_rust_future_cancel_f64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_f64")
    actual external fun ffi_biometrics_rust_future_free_f64(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_f64")
    actual external fun ffi_biometrics_rust_future_complete_f64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Double

    @JvmName("ffi_biometrics_rust_future_poll_pointer")
    actual external fun ffi_biometrics_rust_future_poll_pointer(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_pointer")
    actual external fun ffi_biometrics_rust_future_cancel_pointer(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_pointer")
    actual external fun ffi_biometrics_rust_future_free_pointer(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_pointer")
    actual external fun ffi_biometrics_rust_future_complete_pointer(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Pointer

    @JvmName("ffi_biometrics_rust_future_poll_rust_buffer")
    actual external fun ffi_biometrics_rust_future_poll_rust_buffer(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_rust_buffer")
    actual external fun ffi_biometrics_rust_future_cancel_rust_buffer(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_rust_buffer")
    actual external fun ffi_biometrics_rust_future_free_rust_buffer(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_rust_buffer")
    actual external fun ffi_biometrics_rust_future_complete_rust_buffer(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer

    @JvmName("ffi_biometrics_rust_future_poll_void")
    actual external fun ffi_biometrics_rust_future_poll_void(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_cancel_void")
    actual external fun ffi_biometrics_rust_future_cancel_void(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_free_void")
    actual external fun ffi_biometrics_rust_future_free_void(`handle`: Pointer,
    ): Unit

    @JvmName("ffi_biometrics_rust_future_complete_void")
    actual external fun ffi_biometrics_rust_future_complete_void(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Unit

    @JvmName("uniffi_biometrics_checksum_func_biometrics_result_content")
    actual external fun uniffi_biometrics_checksum_func_biometrics_result_content(
    ): UShort

    @JvmName("uniffi_biometrics_checksum_func_check_support_biometrics")
    actual external fun uniffi_biometrics_checksum_func_check_support_biometrics(
    ): UShort

    @JvmName("ffi_biometrics_uniffi_contract_version")
    actual external fun ffi_biometrics_uniffi_contract_version(
    ): UInt

    
}

// Async support

// Public interface members begin here.










