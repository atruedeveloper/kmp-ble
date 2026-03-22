package com.atruedev.kmpble.codec

import com.atruedev.kmpble.scanner.Advertisement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Decode manufacturer-specific data from this advertisement.
 *
 * Reads directly from [BleData] — zero-copy on iOS (no [BleData.toByteArray]).
 *
 * @param companyId Bluetooth SIG company identifier (e.g., 0x004C for Apple)
 * @param decoder Decodes the manufacturer payload
 * @return Decoded value, or `null` if [companyId] is not present
 */
public fun <T> Advertisement.decodeManufacturerData(
    companyId: Int,
    decoder: BleDataDecoder<T>,
): T? = manufacturerData[companyId]?.let(decoder::decode)

/**
 * Decode service-specific data from this advertisement.
 *
 * Reads directly from [BleData] — zero-copy on iOS (no [BleData.toByteArray]).
 *
 * @param serviceUuid UUID of the service whose data to decode
 * @param decoder Decodes the service data payload
 * @return Decoded value, or `null` if [serviceUuid] is not present
 */
@OptIn(ExperimentalUuidApi::class)
public fun <T> Advertisement.decodeServiceData(
    serviceUuid: Uuid,
    decoder: BleDataDecoder<T>,
): T? = serviceData[serviceUuid]?.let(decoder::decode)
