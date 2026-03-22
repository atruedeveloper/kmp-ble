package com.atruedev.kmpble.dfu.transport

import kotlinx.coroutines.flow.Flow

public interface DfuTransport : AutoCloseable {

    public val mtu: Int

    public val notifications: Flow<ByteArray>

    public suspend fun sendCommand(data: ByteArray): ByteArray

    public suspend fun sendData(data: ByteArray)

    override fun close()
}
