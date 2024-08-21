package reverse_proxy



fun `start`(`backendPort`: UShort, `onReady`: VoidCallback) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_60a8_start(FfiConverterUShort.lower(`backendPort`), FfiConverterTypeVoidCallback.lower(`onReady`), _status)
}


fun `forward`(`newForwardPort`: UShort) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_60a8_forward(FfiConverterUShort.lower(`newForwardPort`), _status)
}
