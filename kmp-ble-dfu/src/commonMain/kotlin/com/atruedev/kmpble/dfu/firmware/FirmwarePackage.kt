package com.atruedev.kmpble.dfu.firmware

public class FirmwarePackage(
    public val initPacket: ByteArray,
    public val firmware: ByteArray,
) {
    public val totalBytes: Long get() = (initPacket.size + firmware.size).toLong()

    public companion object {
        public fun fromZipBytes(zipData: ByteArray): FirmwarePackage =
            NordicDfuZipParser.parse(zipData)
    }
}
