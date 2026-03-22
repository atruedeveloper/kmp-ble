package com.atruedev.kmpble.dfu.transport

import com.atruedev.kmpble.dfu.DfuError
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.Characteristic
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.l2cap.L2capChannel
import com.atruedev.kmpble.peripheral.Peripheral
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn

internal class L2capDfuTransport(
    private val peripheral: Peripheral,
    private val channel: L2capChannel,
) : DfuTransport {

    private val controlPoint: Characteristic = peripheral.findCharacteristic(
        DfuUuids.DFU_SERVICE, DfuUuids.DFU_CONTROL_POINT,
    ) ?: throw DfuError.CharacteristicNotFound("DFU Control Point")

    override val mtu: Int get() = channel.mtu

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
        channel.write(data)
    }

    override fun close() {
        channel.close()
    }
}
