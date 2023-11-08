package httpsproxy



fun `start`(`port`: UShort) =
    
    rustCall() { _status ->
    UniFFILib.httpsproxy_26fe_start(FfiConverterUShort.lower(`port`), _status)
}
