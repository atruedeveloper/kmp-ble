package com.atruedev.kmpble.codec

import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.Characteristic
import com.atruedev.kmpble.gatt.Descriptor
import com.atruedev.kmpble.gatt.Observation
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read a characteristic and decode the result. */
public suspend fun <T> Peripheral.read(
    characteristic: Characteristic,
    decoder: BleDecoder<T>,
): T = decoder.decode(read(characteristic))

/** Encode a value and write it to a characteristic. */
public suspend fun <T> Peripheral.write(
    characteristic: Characteristic,
    value: T,
    encoder: BleEncoder<T>,
    writeType: WriteType = WriteType.WithResponse,
): Unit = write(characteristic, encoder.encode(value), writeType)

/**
 * Observe decoded values from a characteristic with transparent reconnection.
 *
 * Behaves identically to [Peripheral.observeValues] but applies [decoder] to each
 * incoming [ByteArray] before emission. Decode failures propagate as flow exceptions.
 */
public fun <T> Peripheral.observeValues(
    characteristic: Characteristic,
    decoder: BleDecoder<T>,
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<T> = observeValues(characteristic, backpressure).map(decoder::decode)

/**
 * Observe decoded observations from a characteristic, including disconnect events.
 *
 * Behaves identically to [Peripheral.observe] but decodes [Observation.Value] payloads
 * into typed [DecodedObservation.Value] events. [Observation.Disconnected] maps to
 * [DecodedObservation.Disconnected].
 */
public fun <T> Peripheral.observe(
    characteristic: Characteristic,
    decoder: BleDecoder<T>,
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<DecodedObservation<T>> = observe(characteristic, backpressure).map { observation ->
    when (observation) {
        is Observation.Value -> DecodedObservation.Value(decoder.decode(observation.data))
        is Observation.Disconnected -> DecodedObservation.Disconnected
    }
}

/** Read a descriptor and decode the result. */
public suspend fun <T> Peripheral.readDescriptor(
    descriptor: Descriptor,
    decoder: BleDecoder<T>,
): T = decoder.decode(readDescriptor(descriptor))

/** Encode a value and write it to a descriptor. */
public suspend fun <T> Peripheral.writeDescriptor(
    descriptor: Descriptor,
    value: T,
    encoder: BleEncoder<T>,
): Unit = writeDescriptor(descriptor, encoder.encode(value))
