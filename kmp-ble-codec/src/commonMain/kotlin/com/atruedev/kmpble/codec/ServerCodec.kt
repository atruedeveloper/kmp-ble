package com.atruedev.kmpble.codec

import com.atruedev.kmpble.BleData
import com.atruedev.kmpble.Identifier
import com.atruedev.kmpble.server.GattServer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Encode a value and send it as a notification. */
@OptIn(ExperimentalUuidApi::class)
public suspend fun <T> GattServer.notify(
    characteristicUuid: Uuid,
    device: Identifier?,
    value: T,
    encoder: BleEncoder<T>,
): Unit = notify(characteristicUuid, device, BleData(encoder.encode(value)))

/** Encode a value and send it as an indication. */
@OptIn(ExperimentalUuidApi::class)
public suspend fun <T> GattServer.indicate(
    characteristicUuid: Uuid,
    device: Identifier,
    value: T,
    encoder: BleEncoder<T>,
): Unit = indicate(characteristicUuid, device, BleData(encoder.encode(value)))

/** Encode a value directly to [BleData] and send it as a notification. */
@OptIn(ExperimentalUuidApi::class)
public suspend fun <T> GattServer.notify(
    characteristicUuid: Uuid,
    device: Identifier?,
    value: T,
    encoder: BleDataEncoder<T>,
): Unit = notify(characteristicUuid, device, encoder.encode(value))

/** Encode a value directly to [BleData] and send it as an indication. */
@OptIn(ExperimentalUuidApi::class)
public suspend fun <T> GattServer.indicate(
    characteristicUuid: Uuid,
    device: Identifier,
    value: T,
    encoder: BleDataEncoder<T>,
): Unit = indicate(characteristicUuid, device, encoder.encode(value))
