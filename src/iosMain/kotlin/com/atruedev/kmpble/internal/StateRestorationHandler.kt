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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBPeripheral
import kotlin.concurrent.AtomicInt
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

    private var scope: CoroutineScope? = null
    private val persistence = ObservationPersistence()
    private val started = AtomicInt(0)

    /**
     * Start listening for state restoration events.
     * Called once when state restoration is enabled.
     *
     * Also prunes stale NSUserDefaults keys from peripherals that were
     * never explicitly closed in previous sessions.
     */
    internal fun start() {
        if (!started.compareAndSet(0, 1)) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope

        safeLog("prune stale keys") {
            val activeIds = PeripheralRegistry.identifiers()
            persistence.pruneStaleEntries(activeIds)
        }

        newScope.launch {
            CentralManagerProvider.scanDelegate.restoredPeripherals.collect { peripherals ->
                handleRestoredPeripherals(peripherals)
            }
        }
    }

    /**
     * Cancel the restoration collector scope. Prevents zombie collectors
     * if the host app's BLE session ends and restarts.
     */
    internal fun stop() {
        scope?.cancel()
        scope = null
        started.store(0)
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
                scope?.launch {
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
        safeLog("persist observations") { persistence.save(peripheralId, keys) }
    }

    /**
     * Clear persisted observations for a specific peripheral. Called on Peripheral.close().
     */
    internal fun clearPersistedObservations(peripheralId: String) {
        safeLog("clear observations") { persistence.clear(peripheralId) }
    }

    private inline fun safeLog(operation: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logEvent(BleLogEvent.Error(null, "StateRestoration: failed to $operation", e))
        }
    }
}
