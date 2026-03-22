package com.atruedev.kmpble.codec

import com.atruedev.kmpble.BleData
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BleCodecTest {

    private val intEncoder = BleEncoder<Int> { value ->
        byteArrayOf((value shr 8).toByte(), value.toByte())
    }
    private val intDecoder = BleDecoder<Int> { data ->
        (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
    }
    private val intBleDataDecoder = BleDataDecoder<Int> { data ->
        (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
    }

    @Test
    fun encoderProducesByteArray() {
        val result = intEncoder.encode(0x0148)
        assertContentEquals(byteArrayOf(0x01, 0x48), result)
    }

    @Test
    fun decoderProducesTypedValue() {
        val result = intDecoder.decode(byteArrayOf(0x01, 0x48))
        assertEquals(0x0148, result)
    }

    @Test
    fun bleCodecFactoryRoundTrips() {
        val codec = bleCodec(intEncoder, intDecoder)
        val encoded = codec.encode(1000)
        val decoded = codec.decode(encoded)
        assertEquals(1000, decoded)
    }

    @Test
    fun decoderMapTransformsOutput() {
        val stringDecoder = intDecoder.map { it.toString() }
        assertEquals("1000", stringDecoder.decode(byteArrayOf(0x03, 0xE8.toByte()))
        )
    }

    @Test
    fun encoderContramapTransformsInput() {
        val stringEncoder = intEncoder.contramap<String, Int> { it.toInt() }
        assertContentEquals(byteArrayOf(0x03, 0xE8.toByte()), stringEncoder.encode("1000"))
    }

    @Test
    fun bimapTransformsBothDirections() {
        val codec = bleCodec(intEncoder, intDecoder)
        val stringCodec = codec.bimap(
            encode = { s: String -> s.toInt() },
            decode = { i: Int -> i.toString() },
        )

        val encoded = stringCodec.encode("1000")
        val decoded = stringCodec.decode(encoded)
        assertEquals("1000", decoded)
    }

    @Test
    fun lambdaConstructionWorksForEncoder() {
        val encoder: BleEncoder<String> = BleEncoder { it.encodeToByteArray() }
        assertContentEquals("hello".encodeToByteArray(), encoder.encode("hello"))
    }

    @Test
    fun lambdaConstructionWorksForDecoder() {
        val decoder: BleDecoder<String> = BleDecoder { it.decodeToString() }
        assertEquals("hello", decoder.decode("hello".encodeToByteArray()))
    }

    @Test
    fun mapChaining() {
        val decoder = intDecoder
            .map { it * 2 }
            .map { it + 1 }
        assertEquals(2001, decoder.decode(byteArrayOf(0x03, 0xE8.toByte())))
    }

    @Test
    fun contramapChaining() {
        val encoder = intEncoder
            .contramap<Int, Int> { it - 1 }
            .contramap<Int, Int> { it * 2 }
        // input: 501 → *2 → 1002 → -1 → 1001 → encode
        assertContentEquals(byteArrayOf(0x03, 0xE9.toByte()), encoder.encode(501))
    }

    // ── BleDataDecoder ──────────────────────────────────────────────────────

    @Test
    fun bleDataDecoderReadsFromBleData() {
        val bleData = BleData(byteArrayOf(0x01, 0x48))
        assertEquals(0x0148, intBleDataDecoder.decode(bleData))
    }

    @Test
    fun bleDataDecoderLambdaConstruction() {
        val decoder: BleDataDecoder<Byte> = BleDataDecoder { it[0] }
        val bleData = BleData(byteArrayOf(0x42))
        assertEquals(0x42, decoder.decode(bleData))
    }

    @Test
    fun bleDataDecoderMap() {
        val stringDecoder = intBleDataDecoder.map { it.toString() }
        val bleData = BleData(byteArrayOf(0x03, 0xE8.toByte()))
        assertEquals("1000", stringDecoder.decode(bleData))
    }

    @Test
    fun bleDataDecoderMapChaining() {
        val decoder = intBleDataDecoder
            .map { it * 2 }
            .map { it + 1 }
        val bleData = BleData(byteArrayOf(0x03, 0xE8.toByte()))
        assertEquals(2001, decoder.decode(bleData))
    }

    // ── Bridging ────────────────────────────────────────────────────────────

    @Test
    fun bleDecoderAsBleDataDecoder() {
        val bridged = intDecoder.asBleDataDecoder()
        val bleData = BleData(byteArrayOf(0x01, 0x48))
        assertEquals(0x0148, bridged.decode(bleData))
    }

    @Test
    fun bleDataDecoderAsBleDecoder() {
        val bridged = intBleDataDecoder.asBleDecoder()
        assertEquals(0x0148, bridged.decode(byteArrayOf(0x01, 0x48)))
    }

    @Test
    fun bridgingRoundTripPreservesValue() {
        val original = BleDataDecoder<Int> { data ->
            (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
        }
        val roundTripped = original.asBleDecoder().asBleDataDecoder()
        val bleData = BleData(byteArrayOf(0x03, 0xE8.toByte()))
        assertEquals(original.decode(bleData), roundTripped.decode(bleData))
    }
}
