package com.atruedev.kmpble.codec

import com.atruedev.kmpble.BleData

// ── ByteArray-based interfaces ──────────────────────────────────────────────

/**
 * Encodes a value of type [T] into a [ByteArray] for BLE transmission.
 *
 * Contravariant in [T] — an `BleEncoder<Any>` is a valid `BleEncoder<String>`.
 */
public fun interface BleEncoder<in T> {
    public fun encode(value: T): ByteArray
}

/**
 * Decodes a [ByteArray] received from BLE into a value of type [T].
 *
 * Use for Peripheral GATT operations and L2CAP channels where the core API
 * already delivers [ByteArray]. For zero-copy decoding of [BleData] from
 * scanner advertisements or server write handlers, use [BleDataDecoder].
 *
 * Covariant in [T] — a `BleDecoder<String>` is a valid `BleDecoder<Any>`.
 */
public fun interface BleDecoder<out T> {
    public fun decode(data: ByteArray): T
}

/**
 * Bidirectional codec combining [BleEncoder] and [BleDecoder].
 *
 * Invariant in [T] since it both consumes and produces [T].
 */
public interface BleCodec<T> : BleEncoder<T>, BleDecoder<T>

// ── BleData-based decoder ───────────────────────────────────────────────────

/**
 * Decodes a [BleData] into a value of type [T] without copying.
 *
 * On iOS, [BleData] wraps `NSData` from CoreBluetooth with zero-copy.
 * A [BleDataDecoder] reads directly from the platform-native buffer via
 * indexed access ([BleData.get]), avoiding the allocation and `memcpy`
 * that [BleData.toByteArray] would incur.
 *
 * Use this for scanner advertisement data ([Advertisement.manufacturerData],
 * [Advertisement.serviceData]) and server write handlers where [BleData]
 * is the native currency.
 *
 * ```kotlin
 * val decoder = BleDataDecoder<HeartRate> { data ->
 *     val flags = data[0].toInt()
 *     val bpm = if (flags and 0x01 == 0) data[1].toInt() and 0xFF
 *               else (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
 *     HeartRate(bpm)
 * }
 * ```
 */
public fun interface BleDataDecoder<out T> {
    public fun decode(data: BleData): T
}

// ── Factories ───────────────────────────────────────────────────────────────

/** Combine a standalone [BleEncoder] and [BleDecoder] into a [BleCodec]. */
public fun <T> bleCodec(encoder: BleEncoder<T>, decoder: BleDecoder<T>): BleCodec<T> =
    object : BleCodec<T> {
        override fun encode(value: T): ByteArray = encoder.encode(value)
        override fun decode(data: ByteArray): T = decoder.decode(data)
    }

// ── ByteArray decoder composition ───────────────────────────────────────────

/** Transform the output of this decoder. */
public fun <A, B> BleDecoder<A>.map(transform: (A) -> B): BleDecoder<B> =
    BleDecoder { data -> transform(decode(data)) }

/** Transform the input of this encoder. */
public fun <A, B> BleEncoder<B>.contramap(transform: (A) -> B): BleEncoder<A> =
    BleEncoder { value -> encode(transform(value)) }

/** Transform both directions of this codec. */
public fun <A, B> BleCodec<A>.bimap(
    encode: (B) -> A,
    decode: (A) -> B,
): BleCodec<B> = bleCodec(
    encoder = (this as BleEncoder<A>).contramap(encode),
    decoder = (this as BleDecoder<A>).map(decode),
)

// ── BleData decoder composition ─────────────────────────────────────────────

/** Transform the output of this BleData decoder. */
public fun <A, B> BleDataDecoder<A>.map(transform: (A) -> B): BleDataDecoder<B> =
    BleDataDecoder { data -> transform(decode(data)) }

// ── Bridging ────────────────────────────────────────────────────────────────

/**
 * Bridge a [BleDecoder] to work with [BleData] input.
 *
 * Incurs a copy via [BleData.toByteArray]. Use when a serialization library
 * requires [ByteArray] but the data source provides [BleData]. Prefer
 * implementing [BleDataDecoder] directly for zero-copy decoding.
 */
public fun <T> BleDecoder<T>.asBleDataDecoder(): BleDataDecoder<T> =
    BleDataDecoder { data -> decode(data.toByteArray()) }

/**
 * Bridge a [BleDataDecoder] to work with [ByteArray] input.
 *
 * Wraps the [ByteArray] in [BleData] via [BleData] factory (copies on Android,
 * creates NSData on iOS). Use when a [BleDataDecoder] is needed on a
 * [ByteArray]-based path like [Peripheral.read].
 */
public fun <T> BleDataDecoder<T>.asBleDecoder(): BleDecoder<T> =
    BleDecoder { bytes -> decode(BleData(bytes)) }
