package reverse_proxy



fun `start`(`proxyPort`: UShort, `frontendPort`: UShort, `frontendCertsPath`: String, `frontendKeyPath`: String, `backendPort`: UShort) =
    
    rustCall() { _status ->
    UniFFILib.reverse_proxy_2e6f_start(FfiConverterUShort.lower(`proxyPort`), FfiConverterUShort.lower(`frontendPort`), FfiConverterString.lower(`frontendCertsPath`), FfiConverterString.lower(`frontendKeyPath`), FfiConverterUShort.lower(`backendPort`), _status)
}


fun `add`(`a`: Int, `b`: Int): Int {
    return FfiConverterInt.lift(
    rustCall() { _status ->
    UniFFILib.reverse_proxy_2e6f_add(FfiConverterInt.lower(`a`), FfiConverterInt.lower(`b`), _status)
})
}

