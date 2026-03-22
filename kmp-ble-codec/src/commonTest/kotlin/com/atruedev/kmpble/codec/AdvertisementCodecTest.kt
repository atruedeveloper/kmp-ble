package com.atruedev.kmpble.codec

import com.atruedev.kmpble.BleData
import com.atruedev.kmpble.Identifier
import com.atruedev.kmpble.scanner.Advertisement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AdvertisementCodecTest {

    private val intDecoder = BleDataDecoder<Int> { data ->
        (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
    }

    private fun advertisement(
        manufacturerData: Map<Int, BleData> = emptyMap(),
        serviceData: Map<Uuid, BleData> = emptyMap(),
    ) = Advertisement(
        identifier = Identifier("AA:BB:CC:DD:EE:FF"),
        name = "Test",
        rssi = -50,
        txPower = null,
        isConnectable = true,
        serviceUuids = emptyList(),
        manufacturerData = manufacturerData,
        serviceData = serviceData,
        timestampNanos = 0L,
    )

    // ── Manufacturer data ───────────────────────────────────────────────────

    @Test
    fun decodeManufacturerDataPresent() {
        val ad = advertisement(
            manufacturerData = mapOf(0x004C to BleData(byteArrayOf(0x01, 0x48))),
        )
        assertEquals(0x0148, ad.decodeManufacturerData(0x004C, intDecoder))
    }

    @Test
    fun decodeManufacturerDataAbsentReturnsNull() {
        val ad = advertisement()
        assertNull(ad.decodeManufacturerData(0x004C, intDecoder))
    }

    @Test
    fun decodeManufacturerDataWrongKeyReturnsNull() {
        val ad = advertisement(
            manufacturerData = mapOf(0x004C to BleData(byteArrayOf(0x01, 0x48))),
        )
        assertNull(ad.decodeManufacturerData(0x0059, intDecoder))
    }

    @Test
    fun decodeManufacturerDataWithMapTransform() {
        val stringDecoder = intDecoder.map { it.toString() }
        val ad = advertisement(
            manufacturerData = mapOf(0x004C to BleData(byteArrayOf(0x03, 0xE8.toByte()))),
        )
        assertEquals("1000", ad.decodeManufacturerData(0x004C, stringDecoder))
    }

    // ── Service data ────────────────────────────────────────────────────────

    private val hrUuid = Uuid.parse("0000180d-0000-1000-8000-00805f9b34fb")

    @Test
    fun decodeServiceDataPresent() {
        val ad = advertisement(
            serviceData = mapOf(hrUuid to BleData(byteArrayOf(0x01, 0x48))),
        )
        assertEquals(0x0148, ad.decodeServiceData(hrUuid, intDecoder))
    }

    @Test
    fun decodeServiceDataAbsentReturnsNull() {
        val ad = advertisement()
        assertNull(ad.decodeServiceData(hrUuid, intDecoder))
    }

    @Test
    fun decodeServiceDataWrongUuidReturnsNull() {
        val otherUuid = Uuid.parse("0000180f-0000-1000-8000-00805f9b34fb")
        val ad = advertisement(
            serviceData = mapOf(hrUuid to BleData(byteArrayOf(0x01, 0x48))),
        )
        assertNull(ad.decodeServiceData(otherUuid, intDecoder))
    }

    // ── Bridged decoder ─────────────────────────────────────────────────────

    @Test
    fun bridgedByteArrayDecoderWorksOnAdvertisement() {
        val byteArrayDecoder = BleDecoder<Int> { data ->
            (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
        }
        val bridged = byteArrayDecoder.asBleDataDecoder()
        val ad = advertisement(
            manufacturerData = mapOf(0x004C to BleData(byteArrayOf(0x01, 0x48))),
        )
        assertEquals(0x0148, ad.decodeManufacturerData(0x004C, bridged))
    }
}
