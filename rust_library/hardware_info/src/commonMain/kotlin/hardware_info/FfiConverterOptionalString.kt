package hardware_info

import okio.Buffer

object FfiConverterOptionalString: FfiConverterRustBuffer<String?> {
    override fun read(buf: Buffer): String? {
        if (buf.readByte().toInt() == 0) {
            return null
        }
        return FfiConverterString.read(buf)
    }

    override fun allocationSize(value: String?): Int {
        if (value == null) {
            return 1
        } else {
            return 1 + FfiConverterString.allocationSize(value)
        }
    }

    override fun write(value: String?, buf: Buffer) {
        if (value == null) {
            buf.writeByte(0)
        } else {
            buf.writeByte(1)
            FfiConverterString.write(value, buf)
        }
    }
}