package com.atruedev.kmpble.profiles.battery

import com.atruedev.kmpble.ServiceUuid
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.uuidFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
private val BATTERY_LEVEL_UUID = uuidFrom("2A19")

@OptIn(ExperimentalUuidApi::class)
public suspend fun Peripheral.readBatteryLevel(): Int? {
    val char = findCharacteristic(ServiceUuid.BATTERY, BATTERY_LEVEL_UUID) ?: return null
    return parseBatteryLevel(read(char))
}

@OptIn(ExperimentalUuidApi::class)
public fun Peripheral.batteryLevelNotifications(
    backpressure: BackpressureStrategy = BackpressureStrategy.Latest,
): Flow<Int> {
    val char = findCharacteristic(ServiceUuid.BATTERY, BATTERY_LEVEL_UUID) ?: return emptyFlow()
    return observeValues(char, backpressure).mapNotNull { parseBatteryLevel(it) }
}
