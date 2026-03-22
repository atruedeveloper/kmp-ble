package com.atruedev.kmpble.dfu.firmware

import com.atruedev.kmpble.dfu.DfuError

internal object NordicDfuZipParser {

    fun parse(zipBytes: ByteArray): FirmwarePackage {
        val entries = ZipReader.readEntries(zipBytes)
        val entryMap = entries.associateBy { it.name }

        val manifest = entryMap["manifest.json"]
            ?: throw DfuError.FirmwareParseError("manifest.json not found in DFU package")

        val manifestText = manifest.data.decodeToString()
        val datFile = extractFileName(manifestText, "dat_file")
        val binFile = extractFileName(manifestText, "bin_file")

        val initPacket = entryMap[datFile]?.data
            ?: throw DfuError.FirmwareParseError("Init packet '$datFile' not found in DFU package")
        val firmware = entryMap[binFile]?.data
            ?: throw DfuError.FirmwareParseError("Firmware binary '$binFile' not found in DFU package")

        return FirmwarePackage(initPacket = initPacket, firmware = firmware)
    }

    private fun extractFileName(manifestJson: String, key: String): String {
        val pattern = """"$key"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(manifestJson)?.groupValues?.get(1)
            ?: throw DfuError.FirmwareParseError("'$key' not found in manifest.json")
    }
}
