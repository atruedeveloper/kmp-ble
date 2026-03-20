package com.atruedev.kmpble.internal

import com.atruedev.kmpble.Identifier
import com.atruedev.kmpble.gatt.internal.ObservationKey
import com.atruedev.kmpble.gatt.internal.ObservationPersistence
import com.atruedev.kmpble.logging.BleLogEvent
import com.atruedev.kmpble.logging.logEvent
import com.atruedev.kmpble.peripheral.IosPeripheral
import com.atruedev.kmpble.peripheral.internal.PeripheralRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBPeripheral
import kotlin.concurrent.Volatile
import kotlin.uuid.ExperimentalUuidApi

/**
 * Handles iOS Core Bluetooth state restoration.
 *
 * When iOS relaunches the app after termination:
 * 1. willRestoreState provides previously connected CBPeripherals
 * 2. This handler reconstructs IosPeripheral wrappers via PeripheralRegistry
 * 3. Restores persisted observation keys from NSUserDefaults
 * 4. Triggers reconnection and observation re-subscription
 */
@OptIn(ExperimentalUuidApi::class)
internal object StateRestorationHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistence = ObservationPersistence()

    @Volatile
    private var started = false

    /**
     * Start listening for state restoration events.
     * Called once when state restoration is enabled.
     *
     * Also prunes stale NSUserDefaults keys from peripherals that were
     * never explicitly closed in previous sessions.
     */
    internal fun start() {
        if (started) return
        started = true

        pruneStaleKeys()

        scope.launch {
            CentralManagerProvider.scanDelegate.restoredPeripherals.collect { peripherals ->
                handleRestoredPeripherals(peripherals)
            }
        }
    }

    private fun handleRestoredPeripherals(cbPeripherals: List<CBPeripheral>) {
        logEvent(BleLogEvent.StateRestoration(null, "restoring ${cbPeripherals.size} peripheral(s)"))

        for (cbPeripheral in cbPeripherals) {
            val identifier = Identifier(cbPeripheral.identifier.UUIDString)

            val savedObservations = try {
                persistence.restore(identifier.value)
            } catch (e: Exception) {
                logEvent(BleLogEvent.Error(identifier, "StateRestoration: failed to restore observations, clearing", e))
                try { persistence.clear(identifier.value) } catch (_: Exception) {}
                emptySet()
            }

            val peripheral = PeripheralRegistry.getOrCreate(identifier) {
                IosPeripheral(cbPeripheral)
            }

            if (peripheral is IosPeripheral) {
                scope.launch {
                    try {
                        peripheral.restoreFromStateRestoration(savedObservations)
                        logEvent(BleLogEvent.StateRestoration(identifier, "restored successfully"))
                    } catch (e: Exception) {
                        logEvent(BleLogEvent.Error(identifier, "StateRestoration: failed to restore", e))
                    }
                }
            }
        }
    }

    /**
     * Persist current observation keys for a specific peripheral.
     * Called by ObservationManager when observations change.
     */
    internal fun persistObservations(peripheralId: String, keys: Set<ObservationKey>) {
        if (!CentralManagerProvider.isStateRestorationEnabled) return
        try {
            persistence.save(peripheralId, keys)
        } catch (e: Exception) {
            logEvent(BleLogEvent.Error(null, "StateRestoration: failed to persist observations", e))
        }
    }

    /**
     * Clear persisted observations for a specific peripheral. Called on Peripheral.close().
     */
    internal fun clearPersistedObservations(peripheralId: String) {
        try {
            persistence.clear(peripheralId)
        } catch (e: Exception) {
            logEvent(BleLogEvent.Error(null, "StateRestoration: failed to clear observations", e))
        }
    }

    /**
     * Remove stale NSUserDefaults keys from peripherals no longer in the registry.
     * Runs once on startup to prevent unbounded key accumulation.
     */
    private fun pruneStaleKeys() {
        try {
            val activeIds = PeripheralRegistry.identifiers()
            persistence.pruneStaleEntries(activeIds)
        } catch (e: Exception) {
            logEvent(BleLogEvent.Error(null, "StateRestoration: failed to prune stale keys", e))
        }
    }
}
