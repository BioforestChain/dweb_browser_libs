package reverse_proxy

import kotlinx.cinterop.staticCFunction

actual fun ForeignCallbackTypeVoidCallback.toForeignCallback() : ForeignCallback =
    staticCFunction{ handle: Handle, method: Int, args: RustBuffer, outBuf: RustBufferPointer?->
        ForeignCallbackTypeVoidCallback.invoke(handle,method,args, requireNotNull( outBuf))
    }