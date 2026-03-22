package com.atruedev.kmpble.profiles.glucose

import com.atruedev.kmpble.ServiceUuid
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.uuidFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
private val GLUCOSE_MEASUREMENT_UUID = uuidFrom("2A18")

@OptIn(ExperimentalUuidApi::class)
private val GLUCOSE_MEASUREMENT_CONTEXT_UUID = uuidFrom("2A34")

@OptIn(ExperimentalUuidApi::class)
private val GLUCOSE_FEATURE_UUID = uuidFrom("2A51")

@OptIn(ExperimentalUuidApi::class)
public fun Peripheral.glucoseMeasurements(
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<GlucoseMeasurement> {
    val char = findCharacteristic(ServiceUuid.GLUCOSE, GLUCOSE_MEASUREMENT_UUID)
        ?: return emptyFlow()
    return observeValues(char, backpressure).mapNotNull { parseGlucoseMeasurement(it) }
}

@OptIn(ExperimentalUuidApi::class)
public fun Peripheral.glucoseMeasurementContexts(
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<GlucoseMeasurementContext> {
    val char = findCharacteristic(ServiceUuid.GLUCOSE, GLUCOSE_MEASUREMENT_CONTEXT_UUID)
        ?: return emptyFlow()
    return observeValues(char, backpressure).mapNotNull { parseGlucoseMeasurementContext(it) }
}

@OptIn(ExperimentalUuidApi::class)
public suspend fun Peripheral.readGlucoseFeature(): GlucoseFeature? {
    val char = findCharacteristic(ServiceUuid.GLUCOSE, GLUCOSE_FEATURE_UUID) ?: return null
    return parseGlucoseFeature(read(char))
}
