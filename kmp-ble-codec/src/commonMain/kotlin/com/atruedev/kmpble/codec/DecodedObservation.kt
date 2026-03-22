package com.atruedev.kmpble.codec

/**
 * A decoded BLE observation event, mirroring [com.atruedev.kmpble.gatt.Observation]
 * with a typed value instead of raw [ByteArray].
 */
public sealed interface DecodedObservation<out T> {
    public data class Value<T>(val value: T) : DecodedObservation<T> {
        override fun toString(): String = "DecodedObservation.Value($value)"
    }

    public data object Disconnected : DecodedObservation<Nothing>
}
