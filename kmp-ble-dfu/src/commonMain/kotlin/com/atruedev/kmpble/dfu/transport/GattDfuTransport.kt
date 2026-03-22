package com.atruedev.kmpble.dfu.transport

import com.atruedev.kmpble.dfu.DfuError
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.Characteristic
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.coroutineScope

internal class GattDfuTransport(
    private val peripheral: Peripheral,
) : DfuTransport {

    private val controlPoint: Characteristic = peripheral.findCharacteristic(
        DfuUuids.DFU_SERVICE, DfuUuids.DFU_CONTROL_POINT,
    ) ?: throw DfuError.CharacteristicNotFound("DFU Control Point")

    private val dataPacket: Characteristic = peripheral.findCharacteristic(
        DfuUuids.DFU_SERVICE, DfuUuids.DFU_PACKET,
    ) ?: throw DfuError.CharacteristicNotFound("DFU Packet")

    override val mtu: Int get() = peripheral.maximumWriteValueLength.value

    override val notifications: Flow<ByteArray> =
        peripheral.observeValues(controlPoint, BackpressureStrategy.Unbounded)

    override suspend fun sendCommand(data: ByteArray): ByteArray = coroutineScope {
        val notificationChannel = notifications.produceIn(this)
        try {
            peripheral.write(controlPoint, data, WriteType.WithResponse)
            notificationChannel.receive()
        } finally {
            notificationChannel.cancel()
        }
    }

    override suspend fun sendData(data: ByteArray) {
        peripheral.write(dataPacket, data, WriteType.WithoutResponse)
    }

    override fun close() {
        // Peripheral lifecycle is managed externally
    }
}
