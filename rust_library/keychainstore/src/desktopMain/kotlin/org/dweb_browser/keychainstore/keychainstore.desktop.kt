

@file:Suppress("RemoveRedundantBackticks")

package org.dweb_browser.keychainstore

// Common helper code.
//
// Ideally this would live in a separate .kt file where it can be unittested etc
// in isolation, and perhaps even published as a re-useable package.
//
// However, it's important that the details of how this helper code works (e.g. the
// way that different builtin types are passed across the FFI) exactly match what's
// expected by the Rust code on the other side of the interface. In practice right
// now that means coming from the exact some version of `uniffi` that was used to
// compile the Rust component. The easiest way to ensure this is to bundle the Kotlin
// helpers directly inline like we're doing here.

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import okio.utf8Size


internal typealias Pointer = com.sun.jna.Pointer
internal val NullPointer: Pointer? = com.sun.jna.Pointer.NULL
internal fun Pointer.toLong(): Long = Pointer.nativeValue(this)
internal fun kotlin.Long.toPointer() = com.sun.jna.Pointer(this)


@kotlin.jvm.JvmInline
value class ByteBuffer(private val inner: java.nio.ByteBuffer) {
    init {
        inner.order(java.nio.ByteOrder.BIG_ENDIAN)
    }

    fun internal() = inner

    fun limit() = inner.limit()

    fun position() = inner.position()

    fun hasRemaining() = inner.hasRemaining()

    fun get() = inner.get()

    fun get(bytesToRead: Int): ByteArray = ByteArray(bytesToRead).apply(inner::get)

    fun getShort() = inner.getShort()

    fun getInt() = inner.getInt()

    fun getLong() = inner.getLong()

    fun getFloat() = inner.getFloat()

    fun getDouble() = inner.getDouble()



    fun put(value: Byte) {
        inner.put(value)
    }

    fun put(src: ByteArray) {
        inner.put(src)
    }

    fun putShort(value: Short) {
        inner.putShort(value)
    }

    fun putInt(value: Int) {
        inner.putInt(value)
    }

    fun putLong(value: Long) {
        inner.putLong(value)
    }

    fun putFloat(value: Float) {
        inner.putFloat(value)
    }

    fun putDouble(value: Double) {
        inner.putDouble(value)
    }


    fun writeUtf8(value: String) {
        Charsets.UTF_8.newEncoder().run {
            onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
            encode(java.nio.CharBuffer.wrap(value), inner, false)
        }
    }
}
fun RustBuffer.setValue(array: RustBufferByValue) {
    this.data = array.data
    this.len = array.len
    this.capacity = array.capacity
}

internal object RustBufferHelper {
    fun allocValue(size: ULong = 0UL): RustBufferByValue = uniffiRustCall { status ->
        // Note: need to convert the size to a `Long` value to make this work with JVM.
        UniffiLib.INSTANCE.ffi_keychainstore_rustbuffer_alloc(size.toLong(), status)
    }.also {
        if(it.data == null) {
            throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
        }
    }

    fun free(buf: RustBufferByValue) = uniffiRustCall { status ->
        UniffiLib.INSTANCE.ffi_keychainstore_rustbuffer_free(buf, status)
    }
}

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStruct(
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField internal var capacity: Long,
    @JvmField internal var len: Long,
    @JvmField internal var data: Pointer?,
) : Structure() {
    constructor(): this(0.toLong(), 0.toLong(), null)

    class ByValue(
        capacity: Long,
        len: Long,
        data: Pointer?,
    ): RustBuffer(capacity, len, data), Structure.ByValue {
        constructor(): this(0.toLong(), 0.toLong(), null)
    }

    /**
     * The equivalent of the `*mut RustBuffer` type.
     * Required for callbacks taking in an out pointer.
     *
     * Size is the sum of all values in the struct.
     */
    class ByReference(
        capacity: Long,
        len: Long,
        data: Pointer?,
    ): RustBuffer(capacity, len, data), Structure.ByReference {
        constructor(): this(0.toLong(), 0.toLong(), null)
    }
}

typealias RustBuffer = RustBufferStruct
typealias RustBufferByValue = RustBufferStruct.ByValue

internal fun RustBuffer.asByteBuffer(): ByteBuffer? {
    require(this.len <= Int.MAX_VALUE) {
        val length = this.len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

internal fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    require(this.len <= Int.MAX_VALUE) {
        val length = this.len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

internal class RustBufferByReference : com.sun.jna.ptr.ByReference(16)
internal fun RustBufferByReference.setValue(value: RustBufferByValue) {
    // NOTE: The offsets are as they are in the C-like struct.
    val pointer = getPointer()
    pointer.setLong(0, value.capacity)
    pointer.setLong(8, value.len)
    pointer.setPointer(16, value.data)
}
internal fun RustBufferByReference.getValue(): RustBufferByValue {
    val pointer = getPointer()
    val value = RustBufferByValue()
    value.writeField("capacity", pointer.getLong(0))
    value.writeField("len", pointer.getLong(8))
    value.writeField("data", pointer.getLong(16))
    return value
}



// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
internal open class ForeignBytesStruct : Structure() {
    @JvmField internal var len: Int = 0
    @JvmField internal var data: Pointer? = null

    internal class ByValue : ForeignBytes(), Structure.ByValue
}
internal typealias ForeignBytes = ForeignBytesStruct
internal typealias ForeignBytesByValue = ForeignBytesStruct.ByValue

interface FfiConverter<KotlinType, FfiType> {
    // Convert an FFI type to a Kotlin type
    fun lift(value: FfiType): KotlinType

    // Convert an Kotlin type to an FFI type
    fun lower(value: KotlinType): FfiType

    // Read a Kotlin type from a `ByteBuffer`
    fun read(buf: ByteBuffer): KotlinType

    // Calculate bytes to allocate when creating a `RustBuffer`
    //
    // This must return at least as many bytes as the write() function will
    // write. It can return more bytes than needed, for example when writing
    // Strings we can't know the exact bytes needed until we the UTF-8
    // encoding, so we pessimistically allocate the largest size possible (3
    // bytes per codepoint).  Allocating extra bytes is not really a big deal
    // because the `RustBuffer` is short-lived.
    fun allocationSize(value: KotlinType): ULong

    // Write a Kotlin type to a `ByteBuffer`
    fun write(value: KotlinType, buf: ByteBuffer)

    // Lower a value into a `RustBuffer`
    //
    // This method lowers a value into a `RustBuffer` rather than the normal
    // FfiType.  It's used by the callback interface code.  Callback interface
    // returns are always serialized into a `RustBuffer` regardless of their
    // normal FFI type.
    fun lowerIntoRustBuffer(value: KotlinType): RustBufferByValue {
        val rbuf = RustBufferHelper.allocValue(allocationSize(value))
        val bbuf = rbuf.asByteBuffer()!!
        write(value, bbuf)
        return RustBufferByValue(
            capacity = rbuf.capacity,
            len = bbuf.position().toLong(),
            data = rbuf.data,
        )
    }

    // Lift a value from a `RustBuffer`.
    //
    // This here mostly because of the symmetry with `lowerIntoRustBuffer()`.
    // It's currently only used by the `FfiConverterRustBuffer` class below.
    fun liftFromRustBuffer(rbuf: RustBufferByValue): KotlinType {
        val byteBuf = rbuf.asByteBuffer()!!
        try {
           val item = read(byteBuf)
           if (byteBuf.hasRemaining()) {
               throw RuntimeException("junk remaining in buffer after lifting, something is very wrong!!")
           }
           return item
        } finally {
            RustBufferHelper.free(rbuf)
        }
    }
}

// FfiConverter that uses `RustBuffer` as the FfiType
interface FfiConverterRustBuffer<KotlinType>: FfiConverter<KotlinType, RustBufferByValue> {
    override fun lift(value: RustBufferByValue) = liftFromRustBuffer(value)
    override fun lower(value: KotlinType) = lowerIntoRustBuffer(value)
}

internal const val UNIFFI_CALL_SUCCESS = 0.toByte()
internal const val UNIFFI_CALL_ERROR = 1.toByte()
internal const val UNIFFI_CALL_UNEXPECTED_ERROR = 2.toByte()

// Default Implementations
internal fun UniffiRustCallStatus.isSuccess(): Boolean
    = code == UNIFFI_CALL_SUCCESS

internal fun UniffiRustCallStatus.isError(): Boolean
    = code == UNIFFI_CALL_ERROR

internal fun UniffiRustCallStatus.isPanic(): Boolean
    = code == UNIFFI_CALL_UNEXPECTED_ERROR

internal fun UniffiRustCallStatusByValue.isSuccess(): Boolean
    = code == UNIFFI_CALL_SUCCESS

internal fun UniffiRustCallStatusByValue.isError(): Boolean
    = code == UNIFFI_CALL_ERROR

internal fun UniffiRustCallStatusByValue.isPanic(): Boolean
    = code == UNIFFI_CALL_UNEXPECTED_ERROR

// Each top-level error class has a companion object that can lift the error from the call status's rust buffer
interface UniffiRustCallStatusErrorHandler<E> {
    fun lift(errorBuf: RustBufferByValue): E;
}

// Helpers for calling Rust
// In practice we usually need to be synchronized to call this safely, so it doesn't
// synchronize itself

// Call a rust function that returns a Result<>.  Pass in the Error class companion that corresponds to the Err
internal inline fun <U, E: kotlin.Exception> uniffiRustCallWithError(errorHandler: UniffiRustCallStatusErrorHandler<E>, crossinline callback: (UniffiRustCallStatus) -> U): U {
    return UniffiRustCallStatusHelper.withReference() { status ->
        val returnValue = callback(status)
        uniffiCheckCallStatus(errorHandler, status)
        returnValue
    }
}

// Check `status` and throw an error if the call wasn't successful
internal fun<E: kotlin.Exception> uniffiCheckCallStatus(errorHandler: UniffiRustCallStatusErrorHandler<E>, status: UniffiRustCallStatus) {
    if (status.isSuccess()) {
        return
    } else if (status.isError()) {
        throw errorHandler.lift(status.errorBuf)
    } else if (status.isPanic()) {
        // when the rust code sees a panic, it tries to construct a rustbuffer
        // with the message.  but if that code panics, then it just sends back
        // an empty buffer.
        if (status.errorBuf.len > 0) {
            throw InternalException(FfiConverterString.lift(status.errorBuf))
        } else {
            throw InternalException("Rust panic")
        }
    } else {
        throw InternalException("Unknown rust call status: $status.code")
    }
}

// UniffiRustCallStatusErrorHandler implementation for times when we don't expect a CALL_ERROR
object UniffiNullRustCallStatusErrorHandler: UniffiRustCallStatusErrorHandler<InternalException> {
    override fun lift(errorBuf: RustBufferByValue): InternalException {
        RustBufferHelper.free(errorBuf)
        return InternalException("Unexpected CALL_ERROR")
    }
}

// Call a rust function that returns a plain value
internal inline fun <U> uniffiRustCall(crossinline callback: (UniffiRustCallStatus) -> U): U {
    return uniffiRustCallWithError(UniffiNullRustCallStatusErrorHandler, callback)
}

internal inline fun<T> uniffiTraitInterfaceCall(
    callStatus: UniffiRustCallStatus,
    makeCall: () -> T,
    writeReturn: (T) -> Unit,
) {
    try {
        writeReturn(makeCall())
    } catch(e: kotlin.Exception) {
        callStatus.code = UNIFFI_CALL_UNEXPECTED_ERROR
        callStatus.errorBuf = FfiConverterString.lower(e.toString())
    }
}

internal inline fun<T, reified E: Throwable> uniffiTraitInterfaceCallWithError(
    callStatus: UniffiRustCallStatus,
    makeCall: () -> T,
    writeReturn: (T) -> Unit,
    lowerError: (E) -> RustBufferByValue
) {
    try {
        writeReturn(makeCall())
    } catch(e: kotlin.Exception) {
        if (e is E) {
            callStatus.code = UNIFFI_CALL_ERROR
            callStatus.errorBuf = lowerError(e)
        } else {
            callStatus.code = UNIFFI_CALL_UNEXPECTED_ERROR
            callStatus.errorBuf = FfiConverterString.lower(e.toString())
        }
    }
}

@Structure.FieldOrder("code", "errorBuf")
internal open class UniffiRustCallStatusStruct(
    @JvmField internal var code: Byte,
    @JvmField internal var errorBuf: RustBufferByValue,
) : Structure() {
    constructor(): this(0.toByte(), RustBufferByValue())

    internal class ByValue(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByValue {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
    internal class ByReference(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByReference {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
}

internal typealias UniffiRustCallStatus = UniffiRustCallStatusStruct.ByReference
internal typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue

internal object UniffiRustCallStatusHelper {
    fun allocValue() = UniffiRustCallStatusByValue()
    fun <U> withReference(block: (UniffiRustCallStatus) -> U): U {
        val status = UniffiRustCallStatus()
        return block(status)
    }
}

internal class UniffiHandleMap<T: Any> {
    private val map = java.util.concurrent.ConcurrentHashMap<Long, T>()
    private val counter: kotlinx.atomicfu.AtomicLong = kotlinx.atomicfu.atomic(1L)

    val size: Int
        get() = map.size

    // Insert a new object into the handle map and get a handle for it
    fun insert(obj: T): Long {
        val handle = counter.getAndAdd(1)
        map[handle] = obj
        return handle
    }

    // Get an object from the handle map
    fun get(handle: Long): T {
        return map[handle] ?: throw InternalException("UniffiHandleMap.get: Invalid handle")
    }

    // Remove an entry from the handlemap and get the Kotlin object back
    fun remove(handle: Long): T {
        return map.remove(handle) ?: throw InternalException("UniffiHandleMap.remove: Invalid handle")
    }
}

typealias ByteByReference = com.sun.jna.ptr.ByteByReference

typealias DoubleByReference = com.sun.jna.ptr.DoubleByReference

typealias FloatByReference = com.sun.jna.ptr.FloatByReference

typealias IntByReference = com.sun.jna.ptr.IntByReference

typealias LongByReference = com.sun.jna.ptr.LongByReference

typealias PointerByReference = com.sun.jna.ptr.PointerByReference

typealias ShortByReference = com.sun.jna.ptr.ShortByReference

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.

// Define FFI callback types
internal interface UniffiRustFutureContinuationCallback: com.sun.jna.Callback {
    fun callback(`data`: Long,`pollResult`: Byte,)
}
internal interface UniffiForeignFutureFree: com.sun.jna.Callback {
    fun callback(`handle`: Long,)
}
internal interface UniffiCallbackInterfaceFree: com.sun.jna.Callback {
    fun callback(`handle`: Long,)
}
@Structure.FieldOrder("handle", "free")
internal open class UniffiForeignFutureStruct(
    @JvmField internal var `handle`: Long,
    @JvmField internal var `free`: UniffiForeignFutureFree?,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `handle` = 0.toLong(),
        
        `free` = null,
        
    )

    internal class UniffiByValue(
        `handle`: Long,
        `free`: UniffiForeignFutureFree?,
    ): UniffiForeignFuture(`handle`,`free`,), Structure.ByValue
}

internal typealias UniffiForeignFuture = UniffiForeignFutureStruct

internal fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFuture) {
    `handle` = other.`handle`
    `free` = other.`free`
}
internal fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFutureUniffiByValue) {
    `handle` = other.`handle`
    `free` = other.`free`
}

internal typealias UniffiForeignFutureUniffiByValue = UniffiForeignFutureStruct.UniffiByValue
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructU8Struct(
    @JvmField internal var `returnValue`: Byte,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toByte(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Byte,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructU8(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructU8 = UniffiForeignFutureStructU8Struct

internal fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU8UniffiByValue = UniffiForeignFutureStructU8Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteU8: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructU8UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructI8Struct(
    @JvmField internal var `returnValue`: Byte,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toByte(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Byte,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructI8(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructI8 = UniffiForeignFutureStructI8Struct

internal fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI8UniffiByValue = UniffiForeignFutureStructI8Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteI8: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructI8UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructU16Struct(
    @JvmField internal var `returnValue`: Short,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toShort(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Short,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructU16(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructU16 = UniffiForeignFutureStructU16Struct

internal fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU16UniffiByValue = UniffiForeignFutureStructU16Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteU16: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructU16UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructI16Struct(
    @JvmField internal var `returnValue`: Short,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toShort(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Short,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructI16(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructI16 = UniffiForeignFutureStructI16Struct

internal fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI16UniffiByValue = UniffiForeignFutureStructI16Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteI16: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructI16UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructU32Struct(
    @JvmField internal var `returnValue`: Int,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0,
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Int,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructU32(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructU32 = UniffiForeignFutureStructU32Struct

internal fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU32UniffiByValue = UniffiForeignFutureStructU32Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteU32: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructU32UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructI32Struct(
    @JvmField internal var `returnValue`: Int,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0,
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Int,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructI32(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructI32 = UniffiForeignFutureStructI32Struct

internal fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI32UniffiByValue = UniffiForeignFutureStructI32Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteI32: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructI32UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructU64Struct(
    @JvmField internal var `returnValue`: Long,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toLong(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Long,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructU64(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructU64 = UniffiForeignFutureStructU64Struct

internal fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU64UniffiByValue = UniffiForeignFutureStructU64Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteU64: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructU64UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructI64Struct(
    @JvmField internal var `returnValue`: Long,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.toLong(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Long,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructI64(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructI64 = UniffiForeignFutureStructI64Struct

internal fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI64UniffiByValue = UniffiForeignFutureStructI64Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteI64: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructI64UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructF32Struct(
    @JvmField internal var `returnValue`: Float,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.0f,
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Float,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructF32(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructF32 = UniffiForeignFutureStructF32Struct

internal fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructF32UniffiByValue = UniffiForeignFutureStructF32Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteF32: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructF32UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructF64Struct(
    @JvmField internal var `returnValue`: Double,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = 0.0,
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Double,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructF64(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructF64 = UniffiForeignFutureStructF64Struct

internal fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructF64UniffiByValue = UniffiForeignFutureStructF64Struct.UniffiByValue
internal interface UniffiForeignFutureCompleteF64: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructF64UniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructPointerStruct(
    @JvmField internal var `returnValue`: Pointer?,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = NullPointer,
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: Pointer?,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructPointer(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructPointer = UniffiForeignFutureStructPointerStruct

internal fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointer) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointerUniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructPointerUniffiByValue = UniffiForeignFutureStructPointerStruct.UniffiByValue
internal interface UniffiForeignFutureCompletePointer: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructPointerUniffiByValue,)
}
@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureStructRustBufferStruct(
    @JvmField internal var `returnValue`: RustBufferByValue,
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `returnValue` = RustBufferHelper.allocValue(),
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `returnValue`: RustBufferByValue,
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructRustBuffer(`returnValue`,`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructRustBuffer = UniffiForeignFutureStructRustBufferStruct

internal fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBuffer) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBufferUniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructRustBufferUniffiByValue = UniffiForeignFutureStructRustBufferStruct.UniffiByValue
internal interface UniffiForeignFutureCompleteRustBuffer: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructRustBufferUniffiByValue,)
}
@Structure.FieldOrder("callStatus")
internal open class UniffiForeignFutureStructVoidStruct(
    @JvmField internal var `callStatus`: UniffiRustCallStatusByValue,
) : com.sun.jna.Structure() {
    constructor(): this(
        
        `callStatus` = UniffiRustCallStatusHelper.allocValue(),
        
    )

    internal class UniffiByValue(
        `callStatus`: UniffiRustCallStatusByValue,
    ): UniffiForeignFutureStructVoid(`callStatus`,), Structure.ByValue
}

internal typealias UniffiForeignFutureStructVoid = UniffiForeignFutureStructVoidStruct

internal fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoid) {
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoidUniffiByValue) {
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructVoidUniffiByValue = UniffiForeignFutureStructVoidStruct.UniffiByValue
internal interface UniffiForeignFutureCompleteVoid: com.sun.jna.Callback {
    fun callback(`callbackData`: Long,`result`: UniffiForeignFutureStructVoidUniffiByValue,)
}






































































@Synchronized
private fun findLibraryName(componentName: String): String {
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "keychainstore"
}

private inline fun <reified Lib : Library> loadIndirect(
    componentName: String
): Lib {
    return Native.load<Lib>(findLibraryName(componentName), Lib::class.java)
}

// A JNA Library to expose the extern-C FFI definitions.
// This is an implementation detail which will be called internally by the public API.

internal interface UniffiLib : Library {
    companion object {
        internal val INSTANCE: UniffiLib by lazy {
            loadIndirect<UniffiLib>(componentName = "keychainstore")
                .also { lib: UniffiLib ->
                    uniffiCheckContractApiVersion(lib)
                    uniffiCheckApiChecksums(lib)
                    }
        }
        
    }

    fun uniffi_keychainstore_fn_func_keychain_delete_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun uniffi_keychainstore_fn_func_keychain_get_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun uniffi_keychainstore_fn_func_keychain_has_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun uniffi_keychainstore_fn_func_keychain_item_keys(
        `scope`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun uniffi_keychainstore_fn_func_keychain_set_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        `value`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun uniffi_keychainstore_fn_func_keychain_support_enum_keys(
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun ffi_keychainstore_rustbuffer_alloc(
        `size`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun ffi_keychainstore_rustbuffer_from_bytes(
        `bytes`: ForeignBytesByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun ffi_keychainstore_rustbuffer_free(
        `buf`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Unit
    fun ffi_keychainstore_rustbuffer_reserve(
        `buf`: RustBufferByValue,
        `additional`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun ffi_keychainstore_rust_future_poll_u8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_u8(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_u8(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_u8(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun ffi_keychainstore_rust_future_poll_i8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_i8(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_i8(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_i8(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte
    fun ffi_keychainstore_rust_future_poll_u16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_u16(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_u16(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_u16(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Short
    fun ffi_keychainstore_rust_future_poll_i16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_i16(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_i16(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_i16(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Short
    fun ffi_keychainstore_rust_future_poll_u32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_u32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_u32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_u32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Int
    fun ffi_keychainstore_rust_future_poll_i32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_i32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_i32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_i32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Int
    fun ffi_keychainstore_rust_future_poll_u64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_u64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_u64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_u64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Long
    fun ffi_keychainstore_rust_future_poll_i64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_i64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_i64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_i64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Long
    fun ffi_keychainstore_rust_future_poll_f32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_f32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_f32(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_f32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Float
    fun ffi_keychainstore_rust_future_poll_f64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_f64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_f64(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_f64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Double
    fun ffi_keychainstore_rust_future_poll_pointer(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_pointer(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_pointer(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_pointer(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Pointer?
    fun ffi_keychainstore_rust_future_poll_rust_buffer(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_rust_buffer(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_rust_buffer(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_rust_buffer(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue
    fun ffi_keychainstore_rust_future_poll_void(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_cancel_void(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_free_void(
        `handle`: Long,
    ): Unit
    fun ffi_keychainstore_rust_future_complete_void(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Unit
    fun uniffi_keychainstore_checksum_func_keychain_delete_item(
    ): Short
    fun uniffi_keychainstore_checksum_func_keychain_get_item(
    ): Short
    fun uniffi_keychainstore_checksum_func_keychain_has_item(
    ): Short
    fun uniffi_keychainstore_checksum_func_keychain_item_keys(
    ): Short
    fun uniffi_keychainstore_checksum_func_keychain_set_item(
    ): Short
    fun uniffi_keychainstore_checksum_func_keychain_support_enum_keys(
    ): Short
    fun ffi_keychainstore_uniffi_contract_version(
    ): Int
    
}

private fun uniffiCheckContractApiVersion(lib: UniffiLib) {
    // Get the bindings contract version from our ComponentInterface
    val bindings_contract_version = 26
    // Get the scaffolding contract version by calling the into the dylib
    val scaffolding_contract_version = lib.ffi_keychainstore_uniffi_contract_version()
    if (bindings_contract_version != scaffolding_contract_version) {
        throw RuntimeException("UniFFI contract version mismatch: try cleaning and rebuilding your project")
    }
}


private fun uniffiCheckApiChecksums(lib: UniffiLib) {
    if (lib.uniffi_keychainstore_checksum_func_keychain_delete_item() != 4158.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    if (lib.uniffi_keychainstore_checksum_func_keychain_get_item() != 37387.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    if (lib.uniffi_keychainstore_checksum_func_keychain_has_item() != 19872.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    if (lib.uniffi_keychainstore_checksum_func_keychain_item_keys() != 30516.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    if (lib.uniffi_keychainstore_checksum_func_keychain_set_item() != 3466.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    if (lib.uniffi_keychainstore_checksum_func_keychain_support_enum_keys() != 37390.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
}

// Public interface members begin here.



object FfiConverterBoolean: FfiConverter<Boolean, Byte> {
    override fun lift(value: Byte): Boolean {
        return value.toInt() != 0
    }

    override fun read(buf: ByteBuffer): Boolean {
        return lift(buf.get())
    }

    override fun lower(value: Boolean): Byte {
        return if (value) 1.toByte() else 0.toByte()
    }

    override fun allocationSize(value: Boolean) = 1UL

    override fun write(value: Boolean, buf: ByteBuffer) {
        buf.put(lower(value))
    }
}




object FfiConverterString: FfiConverter<String, RustBufferByValue> {
    // Note: we don't inherit from FfiConverterRustBuffer, because we use a
    // special encoding when lowering/lifting.  We can use `RustBuffer.len` to
    // store our length and avoid writing it out to the buffer.
    override fun lift(value: RustBufferByValue): String {
        try {
            require(value.len <= Int.MAX_VALUE) {
        val length = value.len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
            val byteArr =  value.asByteBuffer()!!.get(value.len.toInt())
            return byteArr.decodeToString()
        } finally {
            RustBufferHelper.free(value)
        }
    }

    override fun read(buf: ByteBuffer): String {
        val len = buf.getInt()
        val byteArr = buf.get(len)
        return byteArr.decodeToString()
    }

    override fun lower(value: String): RustBufferByValue {
        return RustBufferHelper.allocValue(value.utf8Size().toULong()).apply {
            asByteBuffer()!!.writeUtf8(value)
        }
    }

    // We aren't sure exactly how many bytes our string will be once it's UTF-8
    // encoded.  Allocate 3 bytes per UTF-16 code unit which will always be
    // enough.
    override fun allocationSize(value: String): ULong {
        val sizeForLength = 4UL
        val sizeForString = value.length.toULong() * 3UL
        return sizeForLength + sizeForString
    }

    override fun write(value: String, buf: ByteBuffer) {
        buf.putInt(value.utf8Size().toInt())
        buf.writeUtf8(value)
    }
}


object FfiConverterByteArray: FfiConverterRustBuffer<ByteArray> {
    override fun read(buf: ByteBuffer): ByteArray {
        val len = buf.getInt()
        val byteArr = buf.get(len)
        return byteArr
    }
    override fun allocationSize(value: ByteArray): ULong {
        return 4UL + value.size.toULong()
    }
    override fun write(value: ByteArray, buf: ByteBuffer) {
        buf.putInt(value.size)
        buf.put(value)
    }
}




object FfiConverterOptionalByteArray: FfiConverterRustBuffer<kotlin.ByteArray?> {
    override fun read(buf: ByteBuffer): kotlin.ByteArray? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterByteArray.read(buf)
    }

    override fun allocationSize(value: kotlin.ByteArray?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterByteArray.allocationSize(value)
        }
    }

    override fun write(value: kotlin.ByteArray?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterByteArray.write(value, buf)
        }
    }
}




object FfiConverterSequenceString: FfiConverterRustBuffer<List<kotlin.String>> {
    override fun read(buf: ByteBuffer): List<kotlin.String> {
        val len = buf.getInt()
        return List<kotlin.String>(len) {
            FfiConverterString.read(buf)
        }
    }

    override fun allocationSize(value: List<kotlin.String>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.sumOf { FfiConverterString.allocationSize(it) }
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<kotlin.String>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach {
            FfiConverterString.write(it, buf)
        }
    }
}


actual fun `keychainDeleteItem`(`scope`: kotlin.String, `key`: kotlin.String): kotlin.Boolean {
    return FfiConverterBoolean.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_delete_item(
            FfiConverterString.lower(`scope`),
            FfiConverterString.lower(`key`),
            uniffiRustCallStatus,
        )
    })
}

actual fun `keychainGetItem`(`scope`: kotlin.String, `key`: kotlin.String): kotlin.ByteArray? {
    return FfiConverterOptionalByteArray.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_get_item(
            FfiConverterString.lower(`scope`),
            FfiConverterString.lower(`key`),
            uniffiRustCallStatus,
        )
    })
}

actual fun `keychainHasItem`(`scope`: kotlin.String, `key`: kotlin.String): kotlin.Boolean {
    return FfiConverterBoolean.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_has_item(
            FfiConverterString.lower(`scope`),
            FfiConverterString.lower(`key`),
            uniffiRustCallStatus,
        )
    })
}

actual fun `keychainItemKeys`(`scope`: kotlin.String): List<kotlin.String> {
    return FfiConverterSequenceString.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_item_keys(
            FfiConverterString.lower(`scope`),
            uniffiRustCallStatus,
        )
    })
}

actual fun `keychainSetItem`(`scope`: kotlin.String, `key`: kotlin.String, `value`: kotlin.ByteArray): kotlin.Boolean {
    return FfiConverterBoolean.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_set_item(
            FfiConverterString.lower(`scope`),
            FfiConverterString.lower(`key`),
            FfiConverterByteArray.lower(`value`),
            uniffiRustCallStatus,
        )
    })
}

actual fun `keychainSupportEnumKeys`(): kotlin.Boolean {
    return FfiConverterBoolean.lift(uniffiRustCall { uniffiRustCallStatus ->
        UniffiLib.INSTANCE.uniffi_keychainstore_fn_func_keychain_support_enum_keys(
            uniffiRustCallStatus,
        )
    })
}


// Async support