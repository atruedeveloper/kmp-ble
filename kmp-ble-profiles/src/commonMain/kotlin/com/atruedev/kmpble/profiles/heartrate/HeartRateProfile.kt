package com.atruedev.kmpble.profiles.heartrate

import com.atruedev.kmpble.ServiceUuid
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.uuidFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
private val HR_MEASUREMENT_UUID = uuidFrom("2A37")

@OptIn(ExperimentalUuidApi::class)
private val BODY_SENSOR_LOCATION_UUID = uuidFrom("2A38")

@OptIn(ExperimentalUuidApi::class)
private val HR_CONTROL_POINT_UUID = uuidFrom("2A39")

private const val RESET_ENERGY_EXPENDED: Byte = 0x01

@OptIn(ExperimentalUuidApi::class)
public fun Peripheral.heartRateMeasurements(
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<HeartRateMeasurement> {
    val char = findCharacteristic(ServiceUuid.HEART_RATE, HR_MEASUREMENT_UUID) ?: return emptyFlow()
    return observeValues(char, backpressure).mapNotNull { parseHeartRateMeasurement(it) }
}

@OptIn(ExperimentalUuidApi::class)
public suspend fun Peripheral.readBodySensorLocation(): BodySensorLocation? {
    val char = findCharacteristic(ServiceUuid.HEART_RATE, BODY_SENSOR_LOCATION_UUID) ?: return null
    return parseBodySensorLocation(read(char))
}

@OptIn(ExperimentalUuidApi::class)
public suspend fun Peripheral.resetHeartRateEnergyExpended() {
    val char = findCharacteristic(ServiceUuid.HEART_RATE, HR_CONTROL_POINT_UUID) ?: return
    write(char, byteArrayOf(RESET_ENERGY_EXPENDED), WriteType.WithResponse)
}
