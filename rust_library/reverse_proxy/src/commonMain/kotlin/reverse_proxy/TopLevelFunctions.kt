package reverse_proxy



fun `start`(`frontendSslPem`: String, `backendPort`: UShort, `onReady`: VoidCallback) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_54a6_start(FfiConverterString.lower(`frontendSslPem`), FfiConverterUShort.lower(`backendPort`), FfiConverterTypeVoidCallback.lower(`onReady`), _status)
}


fun `forward`(`newForwardPort`: UShort) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_54a6_forward(FfiConverterUShort.lower(`newForwardPort`), _status)
}
