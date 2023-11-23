package reverse_proxy



fun `start`(`backendPort`: UShort, `onReady`: VoidCallback) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_6e76_start(FfiConverterUShort.lower(`backendPort`), FfiConverterTypeVoidCallback.lower(`onReady`), _status)
}
