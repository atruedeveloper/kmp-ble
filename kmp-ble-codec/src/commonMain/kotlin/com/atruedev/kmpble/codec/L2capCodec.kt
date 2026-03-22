package com.atruedev.kmpble.codec

import com.atruedev.kmpble.l2cap.L2capChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Decode each incoming L2CAP packet into a typed value. */
public fun <T> L2capChannel.incoming(decoder: BleDecoder<T>): Flow<T> =
    incoming.map(decoder::decode)

/** Encode a value and write it to the L2CAP channel. */
public suspend fun <T> L2capChannel.write(value: T, encoder: BleEncoder<T>): Unit =
    write(encoder.encode(value))
