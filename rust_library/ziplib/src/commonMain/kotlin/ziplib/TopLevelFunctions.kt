package ziplib



fun `decompress`(`zipFilePath`: String, `destPath`: String): ULong {
    return FfiConverterULong.lift(
    rustCall() { _status ->
    UniFFILib.ziplib_d872_decompress(FfiConverterString.lower(`zipFilePath`), FfiConverterString.lower(`destPath`), _status)
})
}

