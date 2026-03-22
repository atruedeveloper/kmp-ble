package com.atruedev.kmpble.dfu.testing

import com.atruedev.kmpble.dfu.transport.DfuTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public class FakeDfuTransport(
    override val mtu: Int = 20,
) : DfuTransport {

    private val _notifications = Channel<ByteArray>(Channel.BUFFERED)
    override val notifications: Flow<ByteArray> = _notifications.receiveAsFlow()

    private val commandLog = mutableListOf<ByteArray>()
    private val dataLog = mutableListOf<ByteArray>()
    private val responseQueue = Channel<ByteArray>(Channel.BUFFERED)
    private var _closed = false

    override suspend fun sendCommand(data: ByteArray): ByteArray {
        commandLog.add(data.copyOf())
        return responseQueue.receive()
    }

    override suspend fun sendData(data: ByteArray) {
        dataLog.add(data.copyOf())
    }

    override fun close() {
        if (_closed) return
        _closed = true
        _notifications.close()
        responseQueue.close()
    }

    public suspend fun enqueueResponse(response: ByteArray) {
        responseQueue.send(response)
    }

    public suspend fun emitNotification(data: ByteArray) {
        _notifications.send(data)
    }

    public fun getCommandLog(): List<ByteArray> = commandLog.toList()

    public fun getDataLog(): List<ByteArray> = dataLog.toList()

    public fun clearLogs() {
        commandLog.clear()
        dataLog.clear()
    }
}
