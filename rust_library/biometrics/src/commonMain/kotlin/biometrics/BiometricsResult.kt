package biometrics


import okio.Buffer

data class BiometricsResult (
    var `success`: Boolean, 
    var `message`: String
) {
    
}

object FfiConverterTypeBiometricsResult: FfiConverterRustBuffer<BiometricsResult> {
    override fun read(buf: Buffer): BiometricsResult {
        return BiometricsResult(
            FfiConverterBoolean.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: BiometricsResult) = (
            FfiConverterBoolean.allocationSize(value.`success`) +
            FfiConverterString.allocationSize(value.`message`)
    )

    override fun write(value: BiometricsResult, buf: Buffer) {
            FfiConverterBoolean.write(value.`success`, buf)
            FfiConverterString.write(value.`message`, buf)
    }
}