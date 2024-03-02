package reverse_proxy

actual fun ForeignCallbackTypeVoidCallback.toForeignCallback() : ForeignCallback =
    NativeCallback(this::invoke)