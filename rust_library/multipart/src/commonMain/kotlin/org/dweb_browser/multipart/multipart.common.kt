

@file:Suppress("RemoveRedundantBackticks")

package org.dweb_browser.multipart

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

class InternalException(message: String) : kotlin.Exception(message)

// Public interface members begin here.


// Interface implemented by anything that can contain an object reference.
//
// Such types expose a `destroy()` method that must be called to cleanly
// dispose of the contained objects. Failure to call this method may result
// in memory leaks.
//
// The easiest way to ensure this method is called is to use the `.use`
// helper method to execute a block and destroy the object at the end.
@OptIn(ExperimentalStdlibApi::class)
interface Disposable : AutoCloseable {
    fun destroy()
    override fun close() = destroy()
    companion object {
        internal fun destroy(vararg args: Any?) {
            for (arg in args) {
                if (arg is Disposable) {
                    arg.destroy()
                }
            }
        }
    }
}

@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T : Disposable?, R> T.use(block: (T) -> R): R {
    kotlin.contracts.contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return try {
        block(this)
    } finally {
        try {
            // N.B. our implementation is on the nullable type `Disposable?`.
            this?.destroy()
        } catch (e: Throwable) {
            // swallow
        }
    }
}

/** Used to instantiate an interface without an actual pointer, for fakes in tests, mostly. */
object NoPointer











interface MultipartConsumer {
    
    fun `onOpen`(`id`: kotlin.Int)
    
    fun `onFieldStart`(`name`: kotlin.String?, `fileName`: kotlin.String?, `contentType`: kotlin.String?, `fieldIndex`: kotlin.Int)
    
    fun `onFieldChunk`(`fieldIndex`: kotlin.Int, `chunk`: kotlin.ByteArray)
    
    fun `onFieldEnd`(`fieldIndex`: kotlin.Int)
    
    fun `onClose`(`id`: kotlin.Int)
    
    companion object
}





expect fun `getBoundary`(`headers`: Map<kotlin.String, kotlin.String>): kotlin.String?

expect fun `processMultipartClose`(`id`: kotlin.Int)

expect fun `processMultipartOpen`(`boundary`: kotlin.String, `consumer`: MultipartConsumer)

expect fun `processMultipartWrite`(`id`: kotlin.Int, `chunk`: kotlin.ByteArray)

