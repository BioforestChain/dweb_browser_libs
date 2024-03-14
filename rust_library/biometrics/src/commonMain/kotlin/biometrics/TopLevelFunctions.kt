package biometrics



fun `checkSupportBiometrics`(): Byte {
    return FfiConverterByte.lift(
    rustCall() { _status ->
    UniFFILib.biometrics_5a15_check_support_biometrics( _status)
})
}



fun `biometricsResultContent`(`reason`: String): BiometricsResult {
    return FfiConverterTypeBiometricsResult.lift(
    rustCall() { _status ->
    UniFFILib.biometrics_5a15_biometrics_result_content(FfiConverterString.lower(`reason`), _status)
})
}

