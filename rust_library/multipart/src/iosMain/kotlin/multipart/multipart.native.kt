// This file was autogenerated by some hot garbage in the `uniffi` crate.
// Trust me, you don't want to mess with it!

@file:Suppress("NAME_SHADOWING")

package multipart

import kotlinx.cinterop.CValue
import kotlinx.cinterop.*
import kotlinx.atomicfu.getAndUpdate
import okio.Buffer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE")
actual typealias Pointer = CPointer<out CPointed>

actual fun kotlin.Long.toPointer(): Pointer = requireNotNull(this.toCPointer())

actual fun Pointer.toLong(): kotlin.Long = this.rawValue.toLong()

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias UBytePointer = CPointer<UByteVar>

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline infix fun kotlin.Byte.and(other: kotlin.Long): kotlin.Long = toLong() and other

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline infix fun kotlin.Byte.and(other: kotlin.Int): kotlin.Int = toInt() and other

// byte twiddling was basically pasted from okio
actual fun UBytePointer.asSource(len: kotlin.Long): NoCopySource = object : NoCopySource {
    var readBytes: kotlin.Int = 0
    var remaining: kotlin.Long = len

    init {
        if (len < 0) {
            throw IllegalStateException("Trying to create NoCopySource with negative length")
        }
    }

    private fun requireLen(requiredLen: kotlin.Long) {
        if (remaining < requiredLen) {
            throw IllegalStateException("Expected at least ${requiredLen} bytes in source but have only ${len}")
        }
        remaining -= requiredLen
    }

    override fun exhausted(): kotlin.Boolean = remaining == 0L

    override fun readByte(): kotlin.Byte {
        requireLen(1)
        return reinterpret<ByteVar>()[readBytes++]
    }

    override fun readShort(): kotlin.Short {
        requireLen(2)
        val data = reinterpret<ByteVar>()
        val s = data[readBytes++] and 0xff shl 8 or (data[readBytes++] and 0xff)
        return s.toShort()
    }

    override fun readInt(): kotlin.Int {
        requireLen(4)
        val data = reinterpret<ByteVar>()
        val i = (
                data[readBytes++] and 0xff shl 24
                        or (data[readBytes++] and 0xff shl 16)
                        or (data[readBytes++] and 0xff shl 8)
                        or (data[readBytes++] and 0xff)
                )
        return i
    }

    override fun readLong(): kotlin.Long {
        requireLen(8)
        val data = reinterpret<ByteVar>()
        val v = (
                data[readBytes++] and 0xffL shl 56
                        or (data[readBytes++] and 0xffL shl 48)
                        or (data[readBytes++] and 0xffL shl 40)
                        or (data[readBytes++] and 0xffL shl 32)
                        or (data[readBytes++] and 0xffL shl 24)
                        or (data[readBytes++] and 0xffL shl 16)
                        or (data[readBytes++] and 0xffL shl 8) // ktlint-disable no-multi-spaces
                        or (data[readBytes++] and 0xffL)
                )
        return v
    }

    override fun readByteArray(): ByteArray = readByteArray(len)

    override fun readByteArray(len: kotlin.Long): ByteArray {
        requireLen(len)

        val cast = reinterpret<ByteVar>()
        val intLen = len.toInt()
        val byteArray = ByteArray(intLen)

        for (writeIdx in 0 until intLen) {
            byteArray[writeIdx] = cast[readBytes++]
        }

        return byteArray
    }
}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBuffer = CValue<multipart.cinterop.RustBuffer>

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBufferPointer = CPointer<multipart.cinterop.RustBuffer>

actual fun RustBuffer.asSource(): NoCopySource {
    val data = useContents { data }
    val len = useContents { len }
    return requireNotNull(data).asSource(len.toLong())
}

actual val RustBuffer.dataSize: kotlin.Int
    get() = useContents { len }

actual fun RustBuffer.free(): Unit =
    rustCall { status: RustCallStatus ->
        UniFFILib.ffi_multipart_rustbuffer_free(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status: RustCallStatus ->
        val size = buffer.size
        UniFFILib.ffi_multipart_rustbuffer_alloc(size.toInt(), status).also {
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

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias ForeignBytes = CValue<multipart.cinterop.ForeignBytes>
// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatus = CPointer<multipart.cinterop.RustCallStatus>

actual val RustCallStatus.statusCode: kotlin.Byte
    get() = pointed.code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = pointed.errorBuf.readValue()

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T =
    memScoped {
        val allocated = alloc<multipart.cinterop.RustCallStatus>().ptr
        block(allocated)
    }

val RustCallStatusByValue.statusCode: kotlin.Byte
    get() = useContents { code }

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatusByValue = CValue<multipart.cinterop.RustCallStatus>

// This is actually common kotlin but inefficient because of the coarse granular locking...
// TODO either create some real implementation or at least measure if protecting the counter
//      with the lock and using a plain Int wouldn't be faster
actual class UniFfiHandleMap<T : Any> {
    private val mapLock = kotlinx.atomicfu.locks.ReentrantLock()
    private val map = HashMap<kotlin.ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter: kotlinx.atomicfu.AtomicInt = kotlinx.atomicfu.atomic(0)

    actual val size: kotlin.Int
        get() = map.size

    actual fun insert(obj: T): kotlin.ULong {
        val handle = counter.getAndIncrement().toULong()
        synchronizedMapAccess { map.put(handle, obj) }
        return handle
    }

    actual fun get(handle: kotlin.ULong): T? {
        return synchronizedMapAccess { map.get(handle) }
    }

    actual fun remove(handle: kotlin.ULong): T? {
        return synchronizedMapAccess { map.remove(handle) }
    }

    fun <T> synchronizedMapAccess(block: () -> T): T {
        mapLock.lock()
        try {
            return block()
        } finally {
            mapLock.unlock()
        }
    }
}

// FFI type for Rust future continuations

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
internal actual typealias UniFfiRustFutureContinuationCallbackType = CPointer<CFunction<(kotlin.ULong, kotlin.Short) -> Unit>>

internal actual fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType =
    staticCFunction<kotlin.ULong, kotlin.Short, Unit> { continuationHandle: kotlin.ULong, pollResult: kotlin.Short ->
        resumeContinutation(continuationHandle, pollResult)
    }

// Contains loading, initialization code,
// and the FFI Function declarations.
actual internal object UniFFILib {
    init {
        FfiConverterTypeMultipartConsumer.register(this)
        
    }

    actual fun uniffi_multipart_fn_init_callback_multipartconsumer(`callbackStub`: ForeignCallback,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.uniffi_multipart_fn_init_callback_multipartconsumer(`callbackStub`,_uniffi_out_err
    ))

    actual fun uniffi_multipart_fn_func_get_boundary(`headers`: RustBuffer,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer =
    requireNotNull(multipart.cinterop.uniffi_multipart_fn_func_get_boundary(`headers`,_uniffi_out_err
    ))

    actual fun uniffi_multipart_fn_func_process_multipart_close(`id`: Int,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.uniffi_multipart_fn_func_process_multipart_close(`id`,_uniffi_out_err
    ))

    actual fun uniffi_multipart_fn_func_process_multipart_open(`boundary`: RustBuffer,`consumer`: ULong,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.uniffi_multipart_fn_func_process_multipart_open(`boundary`,`consumer`,_uniffi_out_err
    ))

    actual fun uniffi_multipart_fn_func_process_multipart_write(`id`: Int,`chunk`: RustBuffer,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.uniffi_multipart_fn_func_process_multipart_write(`id`,`chunk`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rustbuffer_alloc(`size`: Int,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer =
    requireNotNull(multipart.cinterop.ffi_multipart_rustbuffer_alloc(`size`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rustbuffer_from_bytes(`bytes`: ForeignBytes,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer =
    requireNotNull(multipart.cinterop.ffi_multipart_rustbuffer_from_bytes(`bytes`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rustbuffer_free(`buf`: RustBuffer,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rustbuffer_free(`buf`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer =
    requireNotNull(multipart.cinterop.ffi_multipart_rustbuffer_reserve(`buf`,`additional`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_continuation_callback_set(`callback`: UniFfiRustFutureContinuationCallbackType,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_continuation_callback_set(`callback`,
    ))

    actual fun ffi_multipart_rust_future_poll_u8(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_u8(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_u8(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_u8(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_u8(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_u8(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_u8(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UByte =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_u8(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_i8(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_i8(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_i8(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_i8(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_i8(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_i8(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_i8(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Byte =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_i8(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_u16(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_u16(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_u16(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_u16(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_u16(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_u16(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_u16(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UShort =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_u16(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_i16(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_i16(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_i16(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_i16(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_i16(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_i16(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_i16(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Short =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_i16(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_u32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_u32(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_u32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_u32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_u32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_u32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_u32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): UInt =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_u32(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_i32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_i32(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_i32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_i32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_i32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_i32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_i32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Int =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_i32(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_u64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_u64(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_u64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_u64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_u64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_u64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_u64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): ULong =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_u64(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_i64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_i64(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_i64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_i64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_i64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_i64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_i64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Long =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_i64(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_f32(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_f32(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_f32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_f32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_f32(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_f32(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_f32(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Float =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_f32(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_f64(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_f64(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_f64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_f64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_f64(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_f64(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_f64(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Double =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_f64(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_pointer(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_pointer(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_pointer(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_pointer(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_pointer(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_pointer(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_pointer(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Pointer =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_pointer(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_rust_buffer(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_rust_buffer(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_rust_buffer(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_rust_buffer(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_rust_buffer(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_rust_buffer(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_rust_buffer(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): RustBuffer =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_rust_buffer(`handle`,_uniffi_out_err
    ))

    actual fun ffi_multipart_rust_future_poll_void(`handle`: Pointer,`uniffiCallback`: ULong,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_poll_void(`handle`,`uniffiCallback`,
    ))

    actual fun ffi_multipart_rust_future_cancel_void(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_cancel_void(`handle`,
    ))

    actual fun ffi_multipart_rust_future_free_void(`handle`: Pointer,
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_free_void(`handle`,
    ))

    actual fun ffi_multipart_rust_future_complete_void(`handle`: Pointer,_uniffi_out_err: RustCallStatus, 
    ): Unit =
    requireNotNull(multipart.cinterop.ffi_multipart_rust_future_complete_void(`handle`,_uniffi_out_err
    ))

    actual fun uniffi_multipart_checksum_func_get_boundary(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_func_get_boundary(
    ))

    actual fun uniffi_multipart_checksum_func_process_multipart_close(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_func_process_multipart_close(
    ))

    actual fun uniffi_multipart_checksum_func_process_multipart_open(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_func_process_multipart_open(
    ))

    actual fun uniffi_multipart_checksum_func_process_multipart_write(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_func_process_multipart_write(
    ))

    actual fun uniffi_multipart_checksum_method_multipartconsumer_on_open(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_method_multipartconsumer_on_open(
    ))

    actual fun uniffi_multipart_checksum_method_multipartconsumer_on_field_start(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_method_multipartconsumer_on_field_start(
    ))

    actual fun uniffi_multipart_checksum_method_multipartconsumer_on_field_chunk(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_method_multipartconsumer_on_field_chunk(
    ))

    actual fun uniffi_multipart_checksum_method_multipartconsumer_on_field_end(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_method_multipartconsumer_on_field_end(
    ))

    actual fun uniffi_multipart_checksum_method_multipartconsumer_on_close(
    ): UShort =
    requireNotNull(multipart.cinterop.uniffi_multipart_checksum_method_multipartconsumer_on_close(
    ))

    actual fun ffi_multipart_uniffi_contract_version(
    ): UInt =
    requireNotNull(multipart.cinterop.ffi_multipart_uniffi_contract_version(
    ))

    
}

// Async support

// Public interface members begin here.













// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias ForeignCallback = CPointer<CFunction<(kotlin.ULong, kotlin.Int, UBytePointer?, kotlin.Int, RustBufferPointer?) -> kotlin.Int>>

actual fun ForeignCallbackMultipartConsumer.toForeignCallback() : ForeignCallback =
    staticCFunction { handle: Handle, method: kotlin.Int, argsData: UBytePointer?, argLen: kotlin.Int, outBuf: RustBufferPointer? ->
        // *_Nonnull is ignored by cinterop
        ForeignCallbackMultipartConsumer.invoke(handle, method, requireNotNull(argsData), argLen, requireNotNull(outBuf))
    }





