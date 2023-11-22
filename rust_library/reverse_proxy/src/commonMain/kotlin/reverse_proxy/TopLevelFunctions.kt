package reverse_proxy



fun `start`(`frontendCertsPath`: String, `frontendKeyPath`: String, `backendPort`: UShort, `onReady`: VoidCallback) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_e96d_start(FfiConverterString.lower(`frontendCertsPath`), FfiConverterString.lower(`frontendKeyPath`), FfiConverterUShort.lower(`backendPort`), FfiConverterTypeVoidCallback.lower(`onReady`), _status)
}
