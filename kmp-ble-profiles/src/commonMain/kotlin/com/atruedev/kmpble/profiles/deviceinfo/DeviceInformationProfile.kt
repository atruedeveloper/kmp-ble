package com.atruedev.kmpble.profiles.deviceinfo

import com.atruedev.kmpble.ServiceUuid
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.uuidFrom
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private val MANUFACTURER_NAME_UUID = uuidFrom("2A29")

@OptIn(ExperimentalUuidApi::class)
private val MODEL_NUMBER_UUID = uuidFrom("2A24")

@OptIn(ExperimentalUuidApi::class)
private val SERIAL_NUMBER_UUID = uuidFrom("2A25")

@OptIn(ExperimentalUuidApi::class)
private val HARDWARE_REVISION_UUID = uuidFrom("2A27")

@OptIn(ExperimentalUuidApi::class)
private val FIRMWARE_REVISION_UUID = uuidFrom("2A26")

@OptIn(ExperimentalUuidApi::class)
private val SOFTWARE_REVISION_UUID = uuidFrom("2A28")

@OptIn(ExperimentalUuidApi::class)
private val SYSTEM_ID_UUID = uuidFrom("2A23")

@OptIn(ExperimentalUuidApi::class)
private val PNP_ID_UUID = uuidFrom("2A50")

@OptIn(ExperimentalUuidApi::class)
public suspend fun Peripheral.readDeviceInformation(): DeviceInformation {
    val svc = ServiceUuid.DEVICE_INFORMATION
    return DeviceInformation(
        manufacturerName = readStringChar(svc, MANUFACTURER_NAME_UUID),
        modelNumber = readStringChar(svc, MODEL_NUMBER_UUID),
        serialNumber = readStringChar(svc, SERIAL_NUMBER_UUID),
        hardwareRevision = readStringChar(svc, HARDWARE_REVISION_UUID),
        firmwareRevision = readStringChar(svc, FIRMWARE_REVISION_UUID),
        softwareRevision = readStringChar(svc, SOFTWARE_REVISION_UUID),
        systemId = readByteChar(svc, SYSTEM_ID_UUID)?.let(::parseSystemId),
        pnpId = readByteChar(svc, PNP_ID_UUID)?.let(::parsePnpId),
    )
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun Peripheral.readStringChar(serviceUuid: Uuid, charUuid: Uuid): String? {
    val char = findCharacteristic(serviceUuid, charUuid) ?: return null
    val data = read(char)
    return if (data.isNotEmpty()) data.decodeToString() else null
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun Peripheral.readByteChar(serviceUuid: Uuid, charUuid: Uuid): ByteArray? {
    val char = findCharacteristic(serviceUuid, charUuid) ?: return null
    val data = read(char)
    return if (data.isEmpty()) null else data
}
