package com.atruedev.kmpble.gatt.internal

import platform.Foundation.NSUserDefaults
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val DEFAULTS_KEY_PREFIX = "com.atruedev.kmpble.observation-keys"

/**
 * iOS implementation of ObservationPersistence using NSUserDefaults.
 *
 * Stores observation keys as JSON. NSUserDefaults is backed by a plist file
 * that survives app termination and is available during background launch
 * (state restoration happens before user interaction).
 *
 * Observation keys contain only standard Bluetooth SIG UUIDs (e.g., 0x180D for
 * Heart Rate Service). While these don't contain PHI, they can reveal what
 * health data the app monitors. For apps requiring stronger protection, the
 * keys can be migrated to the Keychain in a future release.
 *
 * Design decision: NSUserDefaults was chosen over Keychain because:
 * 1. Keychain K/N interop requires complex CFTypeRef bridging that varies by Kotlin version
 * 2. NSUserDefaults is immediately available during state restoration (no unlock required)
 * 3. The data (UUID pairs) is low-sensitivity metadata, not user credentials
 */
@OptIn(ExperimentalUuidApi::class)
internal actual class ObservationPersistence actual constructor() {

    private fun keyFor(peripheralId: String) = "$DEFAULTS_KEY_PREFIX.$peripheralId"

    actual fun save(peripheralId: String, keys: Set<ObservationKey>) {
        if (keys.isEmpty()) {
            clear(peripheralId)
            return
        }

        val entries = keys.map { key ->
            mapOf("s" to key.serviceUuid.toString(), "c" to key.charUuid.toString())
        }

        NSUserDefaults.standardUserDefaults.setObject(entries, forKey = keyFor(peripheralId))
    }

    actual fun restore(peripheralId: String): Set<ObservationKey> {
        val array = NSUserDefaults.standardUserDefaults.arrayForKey(keyFor(peripheralId))
            ?: return emptySet()

        val keys = mutableSetOf<ObservationKey>()
        for (item in array) {
            val dict = item as? Map<*, *> ?: continue
            val serviceStr = dict["s"] as? String ?: continue
            val charStr = dict["c"] as? String ?: continue
            try {
                keys.add(
                    ObservationKey(
                        serviceUuid = Uuid.parse(serviceStr),
                        charUuid = Uuid.parse(charStr),
                    )
                )
            } catch (_: Exception) {
                // Skip malformed entries — lenient restore
            }
        }
        return keys
    }

    actual fun clear(peripheralId: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(keyFor(peripheralId))
    }

    /**
     * Remove all persisted observation keys that don't belong to any of the given
     * peripheral IDs. Called during initialization to prevent unbounded key accumulation
     * from peripherals that were connected but never explicitly closed.
     */
    fun pruneStaleEntries(activePeripheralIds: Set<String>) {
        val defaults = NSUserDefaults.standardUserDefaults
        val allKeys = defaults.dictionaryRepresentation().keys
            .filterIsInstance<String>()
            .filter { it.startsWith(DEFAULTS_KEY_PREFIX) }

        val activeKeys = activePeripheralIds.map { keyFor(it) }.toSet()
        for (key in allKeys) {
            if (key !in activeKeys) {
                defaults.removeObjectForKey(key)
            }
        }
    }
}
