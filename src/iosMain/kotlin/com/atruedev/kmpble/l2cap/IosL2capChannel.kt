@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpble.l2cap

import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import platform.CoreBluetooth.CBL2CAPChannel
import platform.Foundation.NSStreamStatusAtEnd
import platform.Foundation.NSStreamStatusClosed
import platform.Foundation.NSStreamStatusError

internal class IosL2capChannel(
    private val cbChannel: CBL2CAPChannel,
    private val scope: CoroutineScope,
) : L2capChannel {

    override val psm: Int = cbChannel.PSM.toInt()

    override val mtu: Int = DEFAULT_MTU

    private val _isOpen = MutableStateFlow(true)
    override val isOpen: Boolean get() = _isOpen.value

    private val dataChannel = Channel<ByteArray>(Channel.BUFFERED)
    override val incoming: Flow<ByteArray> = dataChannel.receiveAsFlow()

    private val readJob: Job

    init {
        readJob = scope.launch(Dispatchers.Default) {
            readLoop()
        }
    }

    private suspend fun readLoop() {
        val inputStream = cbChannel.inputStream ?: run {
            _isOpen.value = false
            dataChannel.close()
            return
        }

        val bufferSize = READ_BUFFER_SIZE
        val buffer = ByteArray(bufferSize)

        try {
            while (_isOpen.value) {
                coroutineContext.ensureActive()
                val status = inputStream.streamStatus
                if (status == NSStreamStatusAtEnd || status == NSStreamStatusClosed || status == NSStreamStatusError) {
                    break
                }

                if (inputStream.hasBytesAvailable) {
                    buffer.usePinned { pinned ->
                        val bytesRead = inputStream.read(
                            pinned.addressOf(0).reinterpret<UByteVar>(),
                            bufferSize.toULong(),
                        ).toInt()

                        when {
                            bytesRead > 0 -> dataChannel.trySend(buffer.copyOf(bytesRead))
                            bytesRead < 0 -> return // Error or EOF — exit readLoop
                        }
                    }
                } else {
                    delay(POLL_INTERVAL_MS)
                }
            }
        } finally {
            if (_isOpen.compareAndSet(expect = true, update = false)) {
                closeStreams()
                dataChannel.close()
            }
        }
    }

    override suspend fun write(data: ByteArray) {
        if (!_isOpen.value) {
            throw L2capException.ChannelClosed()
        }

        val outputStream = cbChannel.outputStream
            ?: throw L2capException.WriteFailed("Output stream not available")

        withContext(Dispatchers.Default) {
            data.usePinned { pinned ->
                var totalWritten = 0
                while (totalWritten < data.size) {
                    if (!_isOpen.value) {
                        throw L2capException.ChannelClosed("Channel closed during write")
                    }

                    val written = outputStream.write(
                        pinned.addressOf(totalWritten).reinterpret<UByteVar>(),
                        (data.size - totalWritten).toULong(),
                    ).toInt()

                    if (written < 0) {
                        throw L2capException.WriteFailed(
                            outputStream.streamError?.localizedDescription ?: "Unknown write error",
                        )
                    }

                    totalWritten += written
                    if (written == 0 && totalWritten < data.size) {
                        delay(POLL_INTERVAL_MS)
                    }
                }
            }
        }
    }

    override fun close() {
        if (!_isOpen.compareAndSet(expect = true, update = false)) return
        readJob.cancel()
        closeStreams()
        dataChannel.close()
    }

    private fun closeStreams() {
        cbChannel.inputStream?.close()
        cbChannel.outputStream?.close()
    }

    private companion object {
        const val DEFAULT_MTU = 2048
        const val READ_BUFFER_SIZE = 4096
        const val POLL_INTERVAL_MS = 10L
    }
}
