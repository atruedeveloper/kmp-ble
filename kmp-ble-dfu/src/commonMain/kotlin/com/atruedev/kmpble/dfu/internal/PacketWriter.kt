package com.atruedev.kmpble.dfu.internal

import com.atruedev.kmpble.dfu.protocol.Crc32
import com.atruedev.kmpble.dfu.protocol.DfuChecksum
import com.atruedev.kmpble.dfu.protocol.DfuOpcode
import com.atruedev.kmpble.dfu.transport.DfuTransport
import kotlinx.coroutines.flow.first
import kotlin.math.min

internal class PacketWriter(
    private val transport: DfuTransport,
    private val prnInterval: Int,
) {

    suspend fun writeData(
        data: ByteArray,
        offset: Int = 0,
        onPacketSent: suspend (bytesSent: Int) -> Unit = {},
    ) {
        val packetSize = transport.mtu
        var pos = offset
        var packetCount = 0

        while (pos < data.size) {
            val end = min(pos + packetSize, data.size)
            transport.sendData(data.copyOfRange(pos, end))
            pos = end
            packetCount++

            onPacketSent(pos)

            if (prnInterval > 0 && packetCount % prnInterval == 0 && pos < data.size) {
                transport.notifications.first()
            }
        }
    }
}

internal fun parseChecksumResponse(response: ByteArray): DfuChecksum {
    require(response.size >= 7) { "Checksum response too short: ${response.size} bytes" }
    require(response[0].toInt() == DfuOpcode.RESPONSE) { "Not a response opcode" }
    require(response[1].toInt() == DfuOpcode.CALCULATE_CHECKSUM) { "Not a checksum response" }

    val offset = response.readLittleEndianInt(3)
    val crc32 = response.readLittleEndianUInt(7)
    return DfuChecksum(offset, crc32)
}

internal fun ByteArray.readLittleEndianInt(index: Int): Int =
    (this[index].toInt() and 0xFF) or
        ((this[index + 1].toInt() and 0xFF) shl 8) or
        ((this[index + 2].toInt() and 0xFF) shl 16) or
        ((this[index + 3].toInt() and 0xFF) shl 24)

internal fun ByteArray.readLittleEndianUInt(index: Int): UInt =
    readLittleEndianInt(index).toUInt()

internal fun Int.toLittleEndianBytes(): ByteArray = byteArrayOf(
    (this and 0xFF).toByte(),
    ((this shr 8) and 0xFF).toByte(),
    ((this shr 16) and 0xFF).toByte(),
    ((this shr 24) and 0xFF).toByte(),
)

internal fun Short.toLittleEndianBytes(): ByteArray = byteArrayOf(
    (this.toInt() and 0xFF).toByte(),
    ((this.toInt() shr 8) and 0xFF).toByte(),
)
