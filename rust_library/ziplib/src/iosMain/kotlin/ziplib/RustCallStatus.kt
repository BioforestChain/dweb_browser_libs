package ziplib

import kotlinx.cinterop.*

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatus = CPointer<ziplib.cinterop.RustCallStatus>

actual val RustCallStatus.statusCode: Int
    get() = pointed.code.toInt()
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = pointed.errorBuf.readValue()

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T =
    memScoped {
        val allocated = alloc<ziplib.cinterop.RustCallStatus>().ptr
        block(allocated)
    }