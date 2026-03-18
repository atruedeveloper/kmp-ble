package com.atruedev.kmpble.l2cap

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction over the socket surface that [AndroidL2capChannel] needs.
 *
 * Production code uses [BluetoothL2capSocket]; host tests use a fake
 * backed by piped streams.
 */
internal interface L2capSocket : Closeable {
    val inputStream: InputStream
    val outputStream: OutputStream
    val isConnected: Boolean
    val maxTransmitPacketSize: Int
}
