package com.atruedev.kmpble.codec

import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.scanner.uuidFrom
import com.atruedev.kmpble.testing.FakePeripheral
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class PeripheralCodecTest {

    private val stringEncoder = BleEncoder<String> { it.encodeToByteArray() }
    private val stringDecoder = BleDecoder<String> { it.decodeToString() }

    @Test
    fun readDecodesCharacteristic() = runTest {
        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a37") {
                    properties(read = true)
                    onRead { "hello".encodeToByteArray() }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a37"))!!
        val result: String = peripheral.read(char, stringDecoder)
        assertEquals("hello", result)
    }

    @Test
    fun writeEncodesValue() = runTest {
        var writtenData: ByteArray? = null

        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a39") {
                    properties(write = true)
                    onWrite { data, _ -> writtenData = data }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a39"))!!
        peripheral.write(char, "hello", stringEncoder)
        assertContentEquals("hello".encodeToByteArray(), writtenData)
    }

    @Test
    fun writePassesWriteType() = runTest {
        var writtenType: WriteType? = null

        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a39") {
                    properties(write = true, writeWithoutResponse = true)
                    onWrite { _, type -> writtenType = type }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a39"))!!
        peripheral.write(char, "hello", stringEncoder, WriteType.WithoutResponse)
        assertEquals(WriteType.WithoutResponse, writtenType)
    }

    @Test
    fun observeValuesDecodesStream() = runTest {
        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a37") {
                    properties(notify = true)
                    onObserve {
                        flow {
                            emit("alpha".encodeToByteArray())
                            emit("beta".encodeToByteArray())
                        }
                    }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a37"))!!
        val values = peripheral.observeValues(char, stringDecoder, BackpressureStrategy.Unbounded).toList()

        assertEquals(listOf("alpha", "beta"), values)
    }

    @Test
    fun observeDecodesValuesAndPreservesDisconnects() = runTest {
        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a37") {
                    properties(notify = true)
                    onObserve {
                        flow {
                            emit("data".encodeToByteArray())
                        }
                    }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a37"))!!
        val observations = peripheral.observe(char, stringDecoder, BackpressureStrategy.Unbounded).toList()

        assertEquals(1, observations.size)
        val first = observations[0]
        assertIs<DecodedObservation.Value<String>>(first)
        assertEquals("data", first.value)
    }

    @Test
    fun writeDefaultsToWithResponse() = runTest {
        var writtenType: WriteType? = null

        val peripheral = FakePeripheral {
            service("180d") {
                characteristic("2a39") {
                    properties(write = true)
                    onWrite { _, type -> writtenType = type }
                }
            }
        }
        peripheral.connect()

        val char = peripheral.findCharacteristic(uuidFrom("180d"), uuidFrom("2a39"))!!
        peripheral.write(char, "hello", stringEncoder)
        assertEquals(WriteType.WithResponse, writtenType)
    }
}
