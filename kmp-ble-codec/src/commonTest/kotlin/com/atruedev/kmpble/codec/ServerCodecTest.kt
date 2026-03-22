package com.atruedev.kmpble.codec

import com.atruedev.kmpble.BleData
import com.atruedev.kmpble.Identifier
import com.atruedev.kmpble.testing.FakeGattServer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ServerCodecTest {

    private val device = Identifier("AA:BB:CC:DD:EE:01")
    private val charUuid = Uuid.parse("00002a37-0000-1000-8000-00805f9b34fb")

    @Test
    fun notifyWithBleEncoder() = runTest {
        val server = FakeGattServer()
        server.open()
        server.simulateConnection(device)

        server.notify(charUuid, device, "hello", TestStringEncoder)

        val records = server.getNotifications()
        assertEquals(1, records.size)
        assertContentEquals("hello".encodeToByteArray(), records[0].data.toByteArray())
    }

    @Test
    fun notifyWithBleDataEncoder() = runTest {
        val server = FakeGattServer()
        server.open()
        server.simulateConnection(device)

        server.notify(charUuid, device, 0x0148, TestIntBleDataEncoder)

        val records = server.getNotifications()
        assertEquals(1, records.size)
        assertEquals(0x01, records[0].data[0])
        assertEquals(0x48, records[0].data[1])
    }

    @Test
    fun notifyBroadcast() = runTest {
        val server = FakeGattServer()
        server.open()
        server.simulateConnection(device)

        server.notify(charUuid, null, "broadcast", TestStringEncoder)

        val records = server.getNotifications()
        assertEquals(1, records.size)
        assertContentEquals("broadcast".encodeToByteArray(), records[0].data.toByteArray())
    }

    @Test
    fun indicateWithBleEncoder() = runTest {
        val server = FakeGattServer()
        server.open()
        server.simulateConnection(device)

        server.indicate(charUuid, device, "ack-me", TestStringEncoder)

        val records = server.getIndications()
        assertEquals(1, records.size)
        assertEquals(charUuid, records[0].characteristicUuid)
        assertEquals(device, records[0].device)
        assertContentEquals("ack-me".encodeToByteArray(), records[0].data.toByteArray())
    }

    @Test
    fun indicateWithBleDataEncoder() = runTest {
        val server = FakeGattServer()
        server.open()
        server.simulateConnection(device)

        val encoder = BleDataEncoder<String> { BleData(it.encodeToByteArray()) }
        server.indicate(charUuid, device, "ack-me", encoder)

        val records = server.getIndications()
        assertEquals(1, records.size)
        assertContentEquals("ack-me".encodeToByteArray(), records[0].data.toByteArray())
    }
}
