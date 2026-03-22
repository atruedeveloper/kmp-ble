package com.atruedev.kmpble.codec

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

/** Combine a standalone [BleEncoder] and [BleDecoder] into a [BleCodec]. */
public fun <T> bleCodec(encoder: BleEncoder<T>, decoder: BleDecoder<T>): BleCodec<T> =
    object : BleCodec<T> {
        override fun encode(value: T): ByteArray = encoder.encode(value)
        override fun decode(data: ByteArray): T = decoder.decode(data)
    }

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
