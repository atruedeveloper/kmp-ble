package com.atruedev.kmpble.dfu.firmware

import com.atruedev.kmpble.dfu.DfuError

internal data class ZipEntry(
    val name: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZipEntry) return false
        return name == other.name && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + data.contentHashCode()
}

internal object ZipReader {

    private const val LOCAL_FILE_HEADER_SIG = 0x04034b50
    private const val CENTRAL_DIR_SIG = 0x02014b50

    fun readEntries(zipData: ByteArray): List<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()
        var offset = 0

        while (offset + 4 <= zipData.size) {
            val sig = zipData.readIntLE(offset)
            if (sig != LOCAL_FILE_HEADER_SIG) break

            if (offset + 30 > zipData.size) {
                throw DfuError.FirmwareParseError("Truncated local file header at offset $offset")
            }

            val compressionMethod = zipData.readShortLE(offset + 8)
            val compressedSize = zipData.readIntLE(offset + 18)
            val uncompressedSize = zipData.readIntLE(offset + 22)
            val nameLength = zipData.readShortLE(offset + 26)
            val extraLength = zipData.readShortLE(offset + 28)

            if (compressionMethod != 0) {
                throw DfuError.FirmwareParseError(
                    "Compressed entries not supported (method=$compressionMethod)"
                )
            }

            val nameStart = offset + 30
            if (nameStart + nameLength > zipData.size) {
                throw DfuError.FirmwareParseError("Truncated filename at offset $offset")
            }
            val name = zipData.decodeToString(nameStart, nameStart + nameLength)

            val dataStart = nameStart + nameLength + extraLength
            val dataEnd = dataStart + compressedSize
            if (dataEnd > zipData.size) {
                throw DfuError.FirmwareParseError("Truncated data for entry '$name'")
            }

            if (!name.endsWith("/")) {
                entries.add(ZipEntry(name, zipData.copyOfRange(dataStart, dataEnd)))
            }

            offset = dataEnd
        }

        if (entries.isEmpty()) {
            throw DfuError.FirmwareParseError("No entries found in ZIP")
        }

        return entries
    }

    private fun ByteArray.readIntLE(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)

    private fun ByteArray.readShortLE(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.decodeToString(start: Int, end: Int): String =
        this.copyOfRange(start, end).decodeToString()
}
