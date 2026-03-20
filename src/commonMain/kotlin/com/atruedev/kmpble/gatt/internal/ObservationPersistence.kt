package com.atruedev.kmpble.gatt.internal

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persists observation keys so they survive app termination during iOS state restoration.
 * On Android, this is a no-op.
 *
 * On iOS, observation keys (service UUID + characteristic UUID pairs) are stored
 * in NSUserDefaults as JSON. The data is low-sensitivity metadata (standard BLE
 * UUIDs), not user credentials. See ObservationPersistence.ios.kt for rationale.
 */
@OptIn(ExperimentalUuidApi::class)
internal expect class ObservationPersistence() {
    /**
     * Persist the current set of active observation keys for a specific peripheral.
     * Called whenever observations change (subscribe/unsubscribe).
     */
    fun save(peripheralId: String, keys: Set<ObservationKey>)

    /**
     * Restore previously persisted observation keys for a specific peripheral.
     * Returns empty set if no persisted state exists or deserialization fails.
     */
    fun restore(peripheralId: String): Set<ObservationKey>

    /**
     * Clear persisted observation state for a specific peripheral.
     * Called on Peripheral.close() or when state restoration is disabled.
     */
    fun clear(peripheralId: String)
}
