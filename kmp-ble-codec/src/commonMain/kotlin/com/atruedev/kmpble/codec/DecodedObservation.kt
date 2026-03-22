package com.atruedev.kmpble.codec

/** Typed equivalent of [com.atruedev.kmpble.gatt.Observation]. */
public sealed interface DecodedObservation<out T> {
    public data class Value<T>(val value: T) : DecodedObservation<T>
    public data object Disconnected : DecodedObservation<Nothing>
}

/** Eliminate a [DecodedObservation] by applying the matching function. */
public inline fun <T, R> DecodedObservation<T>.fold(
    onValue: (T) -> R,
    onDisconnected: () -> R,
): R = when (this) {
    is DecodedObservation.Value -> onValue(value)
    is DecodedObservation.Disconnected -> onDisconnected()
}
