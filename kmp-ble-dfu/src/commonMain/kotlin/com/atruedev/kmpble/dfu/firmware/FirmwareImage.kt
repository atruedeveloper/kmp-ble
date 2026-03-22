package com.atruedev.kmpble.dfu.firmware

public data class FirmwareImage(
    val type: Type,
    val data: ByteArray,
) {
    public enum class Type { APPLICATION, SOFTDEVICE, BOOTLOADER, SOFTDEVICE_BOOTLOADER }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirmwareImage) return false
        return type == other.type && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * type.hashCode() + data.contentHashCode()
}
