package biometrics



fun `checkSupportBiometrics`(): Byte {
    return FfiConverterByte.lift(
    rustCall() { _status ->
    UniFFILib.biometrics_bc7f_check_support_biometrics( _status)
})
}



fun `biometricsResultContent`(`reason`: String): Byte {
    return FfiConverterByte.lift(
    rustCall() { _status ->
    UniFFILib.biometrics_bc7f_biometrics_result_content(FfiConverterString.lower(`reason`), _status)
})
}

