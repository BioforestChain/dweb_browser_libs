

@file:Suppress("RemoveRedundantBackticks")
@file:OptIn(ExperimentalForeignApi::class)

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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.useContents
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.value
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.write
import kotlin.coroutines.resume
import platform.posix.memcpy
import okio.utf8Size


internal typealias Pointer = CPointer<out kotlinx.cinterop.CPointed>
internal val NullPointer: Pointer? = null
internal fun Pointer.toLong(): Long = rawValue.toLong()
internal fun kotlin.Long.toPointer(): Pointer = requireNotNull(this.toCPointer())


class ByteBuffer(
    internal val pointer: CPointer<kotlinx.cinterop.ByteVar>,
    internal val capacity: Int,
    internal var position: Int = 0,
) {
    fun position() = position

    fun hasRemaining() = capacity != position

    private fun checkRemaining(bytes: Int) {
        val remaining = capacity - position
        require(bytes <= remaining) { 
            "buffer is exhausted: required: $bytes, remaining: $remaining, capacity: $capacity, position: $position" 
        }
    }

    fun get(): Byte {
        checkRemaining(1)
        return pointer[position++]
    }

    fun get(bytesToRead: Int): ByteArray {
        checkRemaining(bytesToRead)
        val result = ByteArray(bytesToRead)
        if (result.isNotEmpty()) {
            result.usePinned { pinned ->
                memcpy(pinned.addressOf(0), pointer + position, bytesToRead.toULong())
            }
            position += bytesToRead
        }
        return result
    }

    fun getShort(): Short {
        checkRemaining(2)
        return (((pointer[position++].toInt() and 0xff) shl 8)
                or (pointer[position++].toInt() and 0xff)).toShort()
    }

    fun getInt(): Int {
        checkRemaining(4)
        return (((pointer[position++].toInt() and 0xff) shl 24)
                or ((pointer[position++].toInt() and 0xff) shl 16)
                or ((pointer[position++].toInt() and 0xff) shl 8)
                or (pointer[position++].toInt() and 0xff))
    }

    fun getLong(): Long {
        checkRemaining(8)
        return (((pointer[position++].toLong() and 0xffL) shl 56)
                or ((pointer[position++].toLong() and 0xffL) shl 48)
                or ((pointer[position++].toLong() and 0xffL) shl 40)
                or ((pointer[position++].toLong() and 0xffL) shl 32)
                or ((pointer[position++].toLong() and 0xffL) shl 24)
                or ((pointer[position++].toLong() and 0xffL) shl 16)
                or ((pointer[position++].toLong() and 0xffL) shl 8)
                or (pointer[position++].toLong() and 0xffL))
    }

    fun getFloat() = Float.fromBits(getInt())

    fun getDouble() = Double.fromBits(getLong())



    fun put(value: Byte) {
        checkRemaining(1)
        pointer[position++] = value
    }

    fun put(src: ByteArray) {
        checkRemaining(src.size)
        if (src.isNotEmpty()) {
            src.usePinned { pinned ->
                memcpy(pointer + position, pinned.addressOf(0), src.size.toULong())
            }
            position += src.size
        }
    }

    fun putShort(value: Short) {
        checkRemaining(2)
        pointer[position++] = (value.toInt() ushr 8 and 0xff).toByte()
        pointer[position++] = (value.toInt() and 0xff).toByte()
    }

    fun putInt(value: Int) {
        checkRemaining(4)
        pointer[position++] = (value ushr 24 and 0xff).toByte()
        pointer[position++] = (value ushr 16 and 0xff).toByte()
        pointer[position++] = (value ushr 8 and 0xff).toByte()
        pointer[position++] = (value and 0xff).toByte()
    }

    fun putLong(value: Long) {
        checkRemaining(8)
        pointer[position++] = (value ushr 56 and 0xffL).toByte()
        pointer[position++] = (value ushr 48 and 0xffL).toByte()
        pointer[position++] = (value ushr 40 and 0xffL).toByte()
        pointer[position++] = (value ushr 32 and 0xffL).toByte()
        pointer[position++] = (value ushr 24 and 0xffL).toByte()
        pointer[position++] = (value ushr 16 and 0xffL).toByte()
        pointer[position++] = (value ushr 8 and 0xffL).toByte()
        pointer[position++] = (value and 0xffL).toByte()
    }

    fun putFloat(value: Float) = putInt(value.toRawBits())

    fun putDouble(value: Double) = putLong(value.toRawBits())


    fun writeUtf8(value: String) {
        // TODO: prevent allocating a new byte array here
        put(value.encodeToByteArray())
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

typealias RustBuffer = CPointer<keychainstore.cinterop.RustBuffer>

var RustBuffer.capacity: Long
    get() = pointed.capacity
    set(value) { pointed.capacity = value }
var RustBuffer.len: Long
    get() = pointed.len
    set(value) { pointed.len = value }
var RustBuffer.data: Pointer?
    get() = pointed.data
    set(value) { pointed.data = value?.reinterpret() }
fun RustBuffer.asByteBuffer(): ByteBuffer? {
    require(pointed.len <= Int.MAX_VALUE) {
        val length = pointed.len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(
        pointed.data?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null,
        pointed.len.toInt(),
    )
}

typealias RustBufferByValue = CValue<keychainstore.cinterop.RustBuffer>
fun RustBufferByValue(
    capacity: Long,
    len: Long,
    data: Pointer?,
): RustBufferByValue {
    return cValue<keychainstore.cinterop.RustBuffer> {
        this.capacity = capacity
        this.len = len
        this.data = data?.reinterpret()
    }
}
val RustBufferByValue.capacity: Long
    get() = useContents { capacity }
val RustBufferByValue.len: Long
    get() = useContents { len }
val RustBufferByValue.data: Pointer?
    get() = useContents { data }
fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    require(len <= Int.MAX_VALUE) {
        val length = len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(
        data?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null,
        len.toInt(),
    )
}

/**
 * The equivalent of the `*mut RustBuffer` type.
 * Required for callbacks taking in an out pointer.
 *
 * Size is the sum of all values in the struct.
 */
internal typealias RustBufferByReference = CPointer<keychainstore.cinterop.RustBufferByReference>

internal fun RustBufferByReference.setValue(value: RustBufferByValue) {
    pointed.capacity = value.capacity
    pointed.len = value.len
    pointed.data = value.data?.reinterpret()
}
internal fun RustBufferByReference.getValue(): RustBufferByValue
    = pointed.reinterpret<keychainstore.cinterop.RustBuffer>().readValue()


internal typealias ForeignBytes = CPointer<keychainstore.cinterop.ForeignBytes>
internal var ForeignBytes.len: Int
    get() = pointed.len
    set(value) { pointed.len = value }
internal var ForeignBytes.data: Pointer?
    get() = pointed.data
    set(value) { pointed.data = value?.reinterpret() }

internal typealias ForeignBytesByValue = CValue<keychainstore.cinterop.ForeignBytes>
internal val ForeignBytesByValue.len: Int
    get() = useContents { len }
internal val ForeignBytesByValue.data: Pointer?
    get() = useContents { data }

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

internal typealias UniffiRustCallStatus = CPointer<keychainstore.cinterop.UniffiRustCallStatus>
internal var UniffiRustCallStatus.code: Byte
    get() = pointed.code
    set(value) { pointed.code = value }
internal var UniffiRustCallStatus.errorBuf: RustBufferByValue
    get() = pointed.errorBuf.readValue()
    set(value) { value.place(pointed.errorBuf.ptr) }

internal typealias UniffiRustCallStatusByValue = CValue<keychainstore.cinterop.UniffiRustCallStatus>
fun UniffiRustCallStatusByValue(
    code: Byte,
    errorBuf: RustBufferByValue
): UniffiRustCallStatusByValue {
    return cValue<keychainstore.cinterop.UniffiRustCallStatus> {
        this.code = code
        errorBuf.write(this.errorBuf.rawPtr)
    }
}
internal val UniffiRustCallStatusByValue.code: Byte
    get() = useContents { code }
internal val UniffiRustCallStatusByValue.errorBuf: RustBufferByValue
    get() = useContents { errorBuf.readValue() }

internal object UniffiRustCallStatusHelper {
    fun allocValue() = cValue<keychainstore.cinterop.UniffiRustCallStatus>()
    fun <U> withReference(
        block: (UniffiRustCallStatus) -> U
    ): U {
        return memScoped {
            val status = alloc<keychainstore.cinterop.UniffiRustCallStatus>()
            block(status.ptr)
        }
    }
}

internal class UniffiHandleMap<T: Any> {
    private val mapLock = kotlinx.atomicfu.locks.ReentrantLock()
    private val map = HashMap<Long, T>()

    // We'll start at 1L to prevent "Null Pointers" in native's `interpretCPointer`
    private val counter: kotlinx.atomicfu.AtomicLong = kotlinx.atomicfu.atomic(1L)

    val size: Int
        get() = map.size

    // Insert a new object into the handle map and get a handle for it
    fun insert(obj: T): Long {
        val handle = counter.getAndAdd(1)
        syncAccess { map.put(handle, obj) }
        return handle
    }

    // Get an object from the handle map
    fun get(handle: Long): T {
        return syncAccess { map.get(handle) } ?: throw InternalException("UniffiHandleMap.get: Invalid handle")
    }

    // Remove an entry from the handlemap and get the Kotlin object back
    fun remove(handle: Long): T {
        return syncAccess { map.remove(handle) } ?: throw InternalException("UniffiHandleMap.remove: Invalid handle")
    }

    fun <T> syncAccess(block: () -> T): T {
        mapLock.lock()
        try {
            return block()
        } finally {
            mapLock.unlock()
        }
    }
}

typealias ByteByReference = CPointer<ByteVar>
fun ByteByReference.setValue(value: Byte) {
    this.pointed.value = value
}
fun ByteByReference.getValue() : Byte {
    return this.pointed.value
}

typealias DoubleByReference = CPointer<DoubleVar>
fun DoubleByReference.setValue(value: Double) {
    this.pointed.value = value
}
fun DoubleByReference.getValue() : Double {
    return this.pointed.value
}

typealias FloatByReference = CPointer<FloatVar>
fun FloatByReference.setValue(value: Float) {
    this.pointed.value = value
}
fun FloatByReference.getValue() : Float {
    return this.pointed.value
}

typealias IntByReference = CPointer<IntVar>
fun IntByReference.setValue(value: Int) {
    this.pointed.value = value
}
fun IntByReference.getValue() : Int {
    return this.pointed.value
}

typealias LongByReference = CPointer<LongVar>
fun LongByReference.setValue(value: Long) {
    this.pointed.value = value
}
fun LongByReference.getValue() : Long {
    return this.pointed.value
}

typealias PointerByReference = CPointer<COpaquePointerVar>
fun PointerByReference.setValue(value: Pointer?) {
    this.pointed.value = value
}
fun PointerByReference.getValue(): Pointer? {
    return this.pointed.value
}

typealias ShortByReference = CPointer<ShortVar>
fun ShortByReference.setValue(value: Short) {
    this.pointed.value = value
}
fun ShortByReference.getValue(): Short {
    return this.pointed.value
}

// Contains loading, initialization code,
// and the FFI Function declarations.

internal typealias UniffiRustFutureContinuationCallback = keychainstore.cinterop.UniffiRustFutureContinuationCallback
internal typealias UniffiForeignFutureFree = keychainstore.cinterop.UniffiForeignFutureFree
internal typealias UniffiCallbackInterfaceFree = keychainstore.cinterop.UniffiCallbackInterfaceFree
internal typealias UniffiForeignFuture = CPointer<keychainstore.cinterop.UniffiForeignFuture>

internal var UniffiForeignFuture.`handle`: Long
    get() = pointed.`handle`
    set(value) {
        pointed.`handle` = value
    }

internal var UniffiForeignFuture.`free`: UniffiForeignFutureFree?
    get() = pointed.`free`
    set(value) {
        pointed.`free` = value
    }


internal fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFuture) {
    `handle` = other.`handle`
    `free` = other.`free`
}
internal fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFutureUniffiByValue) {
    `handle` = other.`handle`
    `free` = other.`free`
}

internal typealias UniffiForeignFutureUniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFuture>
fun UniffiForeignFutureUniffiByValue(
    `handle`: Long,
    `free`: UniffiForeignFutureFree?,
): UniffiForeignFutureUniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFuture> {
        this.`handle` = `handle`
        this.`free` = `free`
    }
}


internal val UniffiForeignFutureUniffiByValue.`handle`: Long
    get() = useContents { `handle` }

internal val UniffiForeignFutureUniffiByValue.`free`: UniffiForeignFutureFree?
    get() = useContents { `free` }

internal typealias UniffiForeignFutureStructU8 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructU8>

internal var UniffiForeignFutureStructU8.`returnValue`: Byte
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructU8.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU8UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructU8>
fun UniffiForeignFutureStructU8UniffiByValue(
    `returnValue`: Byte,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU8UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructU8> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructU8UniffiByValue.`returnValue`: Byte
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructU8UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteU8 = keychainstore.cinterop.UniffiForeignFutureCompleteU8
internal typealias UniffiForeignFutureStructI8 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructI8>

internal var UniffiForeignFutureStructI8.`returnValue`: Byte
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructI8.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI8UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructI8>
fun UniffiForeignFutureStructI8UniffiByValue(
    `returnValue`: Byte,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI8UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructI8> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructI8UniffiByValue.`returnValue`: Byte
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructI8UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteI8 = keychainstore.cinterop.UniffiForeignFutureCompleteI8
internal typealias UniffiForeignFutureStructU16 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructU16>

internal var UniffiForeignFutureStructU16.`returnValue`: Short
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructU16.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU16UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructU16>
fun UniffiForeignFutureStructU16UniffiByValue(
    `returnValue`: Short,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU16UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructU16> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructU16UniffiByValue.`returnValue`: Short
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructU16UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteU16 = keychainstore.cinterop.UniffiForeignFutureCompleteU16
internal typealias UniffiForeignFutureStructI16 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructI16>

internal var UniffiForeignFutureStructI16.`returnValue`: Short
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructI16.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI16UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructI16>
fun UniffiForeignFutureStructI16UniffiByValue(
    `returnValue`: Short,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI16UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructI16> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructI16UniffiByValue.`returnValue`: Short
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructI16UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteI16 = keychainstore.cinterop.UniffiForeignFutureCompleteI16
internal typealias UniffiForeignFutureStructU32 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructU32>

internal var UniffiForeignFutureStructU32.`returnValue`: Int
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructU32.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU32UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructU32>
fun UniffiForeignFutureStructU32UniffiByValue(
    `returnValue`: Int,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU32UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructU32> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructU32UniffiByValue.`returnValue`: Int
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructU32UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteU32 = keychainstore.cinterop.UniffiForeignFutureCompleteU32
internal typealias UniffiForeignFutureStructI32 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructI32>

internal var UniffiForeignFutureStructI32.`returnValue`: Int
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructI32.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI32UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructI32>
fun UniffiForeignFutureStructI32UniffiByValue(
    `returnValue`: Int,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI32UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructI32> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructI32UniffiByValue.`returnValue`: Int
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructI32UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteI32 = keychainstore.cinterop.UniffiForeignFutureCompleteI32
internal typealias UniffiForeignFutureStructU64 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructU64>

internal var UniffiForeignFutureStructU64.`returnValue`: Long
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructU64.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructU64UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructU64>
fun UniffiForeignFutureStructU64UniffiByValue(
    `returnValue`: Long,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU64UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructU64> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructU64UniffiByValue.`returnValue`: Long
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructU64UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteU64 = keychainstore.cinterop.UniffiForeignFutureCompleteU64
internal typealias UniffiForeignFutureStructI64 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructI64>

internal var UniffiForeignFutureStructI64.`returnValue`: Long
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructI64.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructI64UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructI64>
fun UniffiForeignFutureStructI64UniffiByValue(
    `returnValue`: Long,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI64UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructI64> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructI64UniffiByValue.`returnValue`: Long
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructI64UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteI64 = keychainstore.cinterop.UniffiForeignFutureCompleteI64
internal typealias UniffiForeignFutureStructF32 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructF32>

internal var UniffiForeignFutureStructF32.`returnValue`: Float
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructF32.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructF32UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructF32>
fun UniffiForeignFutureStructF32UniffiByValue(
    `returnValue`: Float,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructF32UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructF32> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructF32UniffiByValue.`returnValue`: Float
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructF32UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteF32 = keychainstore.cinterop.UniffiForeignFutureCompleteF32
internal typealias UniffiForeignFutureStructF64 = CPointer<keychainstore.cinterop.UniffiForeignFutureStructF64>

internal var UniffiForeignFutureStructF64.`returnValue`: Double
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructF64.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64UniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructF64UniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructF64>
fun UniffiForeignFutureStructF64UniffiByValue(
    `returnValue`: Double,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructF64UniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructF64> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructF64UniffiByValue.`returnValue`: Double
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructF64UniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteF64 = keychainstore.cinterop.UniffiForeignFutureCompleteF64
internal typealias UniffiForeignFutureStructPointer = CPointer<keychainstore.cinterop.UniffiForeignFutureStructPointer>

internal var UniffiForeignFutureStructPointer.`returnValue`: Pointer?
    get() = pointed.`returnValue`
    set(value) {
        pointed.`returnValue` = value
    }

internal var UniffiForeignFutureStructPointer.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointer) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointerUniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructPointerUniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructPointer>
fun UniffiForeignFutureStructPointerUniffiByValue(
    `returnValue`: Pointer?,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructPointerUniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructPointer> {
        this.`returnValue` = `returnValue`
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructPointerUniffiByValue.`returnValue`: Pointer?
    get() = useContents { `returnValue` }

internal val UniffiForeignFutureStructPointerUniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompletePointer = keychainstore.cinterop.UniffiForeignFutureCompletePointer
internal typealias UniffiForeignFutureStructRustBuffer = CPointer<keychainstore.cinterop.UniffiForeignFutureStructRustBuffer>

internal var UniffiForeignFutureStructRustBuffer.`returnValue`: RustBufferByValue
    get() = pointed.`returnValue`.readValue()
    set(value) {
        value.write(pointed.`returnValue`.rawPtr)
    }

internal var UniffiForeignFutureStructRustBuffer.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBuffer) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBufferUniffiByValue) {
    `returnValue` = other.`returnValue`
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructRustBufferUniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructRustBuffer>
fun UniffiForeignFutureStructRustBufferUniffiByValue(
    `returnValue`: RustBufferByValue,
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructRustBufferUniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructRustBuffer> {
        `returnValue`.write(this.`returnValue`.rawPtr)
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructRustBufferUniffiByValue.`returnValue`: RustBufferByValue
    get() = useContents { `returnValue`.readValue() }

internal val UniffiForeignFutureStructRustBufferUniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteRustBuffer = keychainstore.cinterop.UniffiForeignFutureCompleteRustBuffer
internal typealias UniffiForeignFutureStructVoid = CPointer<keychainstore.cinterop.UniffiForeignFutureStructVoid>

internal var UniffiForeignFutureStructVoid.`callStatus`: UniffiRustCallStatusByValue
    get() = pointed.`callStatus`.readValue()
    set(value) {
        value.write(pointed.`callStatus`.rawPtr)
    }


internal fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoid) {
    `callStatus` = other.`callStatus`
}
internal fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoidUniffiByValue) {
    `callStatus` = other.`callStatus`
}

internal typealias UniffiForeignFutureStructVoidUniffiByValue = CValue<keychainstore.cinterop.UniffiForeignFutureStructVoid>
fun UniffiForeignFutureStructVoidUniffiByValue(
    `callStatus`: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructVoidUniffiByValue {
    return cValue<keychainstore.cinterop.UniffiForeignFutureStructVoid> {
        `callStatus`.write(this.`callStatus`.rawPtr)
    }
}


internal val UniffiForeignFutureStructVoidUniffiByValue.`callStatus`: UniffiRustCallStatusByValue
    get() = useContents { `callStatus`.readValue() }

internal typealias UniffiForeignFutureCompleteVoid = keychainstore.cinterop.UniffiForeignFutureCompleteVoid






































































internal interface UniffiLib {
    companion object {
        internal val INSTANCE: UniffiLib by lazy {
            UniffiLibInstance()
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

internal class UniffiLibInstance: UniffiLib {
    override fun uniffi_keychainstore_fn_func_keychain_delete_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_delete_item(
        `scope`,
        `key`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_fn_func_keychain_get_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_get_item(
        `scope`,
        `key`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_fn_func_keychain_has_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_has_item(
        `scope`,
        `key`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_fn_func_keychain_item_keys(
        `scope`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_item_keys(
        `scope`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_fn_func_keychain_set_item(
        `scope`: RustBufferByValue,
        `key`: RustBufferByValue,
        `value`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_set_item(
        `scope`,
        `key`,
        `value`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_fn_func_keychain_support_enum_keys(
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.uniffi_keychainstore_fn_func_keychain_support_enum_keys(
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rustbuffer_alloc(
        `size`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.ffi_keychainstore_rustbuffer_alloc(
        `size`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rustbuffer_from_bytes(
        `bytes`: ForeignBytesByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.ffi_keychainstore_rustbuffer_from_bytes(
        `bytes`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rustbuffer_free(
        `buf`: RustBufferByValue,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rustbuffer_free(
        `buf`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rustbuffer_reserve(
        `buf`: RustBufferByValue,
        `additional`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.ffi_keychainstore_rustbuffer_reserve(
        `buf`,
        `additional`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_u8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_u8(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_u8(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_u8(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_u8(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_u8(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_u8(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_u8(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_i8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_i8(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_i8(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_i8(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_i8(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_i8(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_i8(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Byte = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_i8(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_u16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_u16(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_u16(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_u16(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_u16(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_u16(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_u16(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Short = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_u16(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_i16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_i16(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_i16(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_i16(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_i16(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_i16(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_i16(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Short = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_i16(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_u32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_u32(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_u32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_u32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_u32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_u32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_u32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Int = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_u32(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_i32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_i32(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_i32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_i32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_i32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_i32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_i32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Int = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_i32(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_u64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_u64(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_u64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_u64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_u64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_u64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_u64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Long = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_u64(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_i64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_i64(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_i64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_i64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_i64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_i64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_i64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Long = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_i64(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_f32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_f32(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_f32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_f32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_f32(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_f32(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_f32(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Float = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_f32(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_f64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_f64(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_f64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_f64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_f64(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_f64(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_f64(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Double = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_f64(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_pointer(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_pointer(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_pointer(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_pointer(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_pointer(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_pointer(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_pointer(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Pointer? = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_pointer(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_rust_buffer(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_rust_buffer(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_rust_buffer(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_rust_buffer(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_rust_buffer(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_rust_buffer(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_rust_buffer(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): RustBufferByValue = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_rust_buffer(
        `handle`,
        uniffiCallStatus,
    )
    override fun ffi_keychainstore_rust_future_poll_void(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_poll_void(
        `handle`,
        `callback`,
        `callbackData`,
    )
    override fun ffi_keychainstore_rust_future_cancel_void(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_cancel_void(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_free_void(
        `handle`: Long,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_free_void(
        `handle`,
    )
    override fun ffi_keychainstore_rust_future_complete_void(
        `handle`: Long,
        uniffiCallStatus: UniffiRustCallStatus,
    ): Unit = keychainstore.cinterop.ffi_keychainstore_rust_future_complete_void(
        `handle`,
        uniffiCallStatus,
    )
    override fun uniffi_keychainstore_checksum_func_keychain_delete_item(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_delete_item(
    )
    override fun uniffi_keychainstore_checksum_func_keychain_get_item(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_get_item(
    )
    override fun uniffi_keychainstore_checksum_func_keychain_has_item(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_has_item(
    )
    override fun uniffi_keychainstore_checksum_func_keychain_item_keys(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_item_keys(
    )
    override fun uniffi_keychainstore_checksum_func_keychain_set_item(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_set_item(
    )
    override fun uniffi_keychainstore_checksum_func_keychain_support_enum_keys(
    ): Short = keychainstore.cinterop.uniffi_keychainstore_checksum_func_keychain_support_enum_keys(
    )
    override fun ffi_keychainstore_uniffi_contract_version(
    ): Int = keychainstore.cinterop.ffi_keychainstore_uniffi_contract_version(
    )
    
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