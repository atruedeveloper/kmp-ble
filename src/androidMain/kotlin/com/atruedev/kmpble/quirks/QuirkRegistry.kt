package com.atruedev.kmpble.quirks

import java.util.ServiceLoader

/**
 * Resolves device-specific BLE quirks in priority order:
 * 1. User overrides (registered via [register])
 * 2. Provider entries (contributed via [QuirkProvider])
 * 3. [QuirkKey.default]
 */
public class QuirkRegistry @PublishedApi internal constructor(
    @PublishedApi internal val device: DeviceInfo,
) {
    @PublishedApi
    internal val entries: MutableList<QuirkEntry<*>> = mutableListOf()

    /**
     * Register a quirk for devices matching [match].
     * Later registrations for the same key take priority (checked first).
     */
    public fun <T : Any> register(key: QuirkKey<T>, value: T, match: (DeviceInfo) -> Boolean) {
        entries.add(0, QuirkEntry(key) { device -> if (match(device)) value else null })
    }

    /** Register a quirk using a hierarchical device key (e.g. `"manufacturer:model"`). */
    public fun <T : Any> register(key: QuirkKey<T>, value: T, deviceKey: String) {
        register(key, value) { DeviceMatch.matchesAny(it, setOf(deviceKey)) }
    }

    /** Register a quirk with a device-key-to-value map. Uses hierarchical matching. */
    public fun <T : Any> register(key: QuirkKey<T>, entries: Map<String, T>) {
        this.entries.add(QuirkEntry(key) { device -> DeviceMatch.matchFirst(device, entries) })
    }

    public inline fun <reified T : Any> resolve(key: QuirkKey<T>): T {
        for (entry in entries) {
            if (entry.key !== key) continue
            val result = entry.resolve(device) as? T ?: continue
            return result
        }
        return key.default
    }

    public fun addProvider(provider: QuirkProvider) {
        provider.contribute(this)
    }

    /** Human-readable summary of active quirks for connection-time logging. */
    public fun describe(): String {
        val active = buildList {
            if (resolve(BleQuirks.BondBeforeConnect)) add("bond-before-connect")
            if (resolve(BleQuirks.RefreshServicesOnBond)) add("refresh-services-on-bond")
            val retryCount = resolve(BleQuirks.GattRetryCount)
            if (retryCount > 1) add("retry=${retryCount}x@${resolve(BleQuirks.GattRetryDelay)}")
            val bondTimeout = resolve(BleQuirks.BondStateTimeout)
            if (bondTimeout != BleQuirks.BondStateTimeout.default) add("bond-timeout=$bondTimeout")
            val connTimeout = resolve(BleQuirks.ConnectionTimeout)
            if (connTimeout != BleQuirks.ConnectionTimeout.default) add("conn-timeout=$connTimeout")
        }
        val suffix = if (active.isEmpty()) "no device-specific quirks" else active.joinToString()
        return "${device.manufacturer}/${device.model} — $suffix"
    }

    public companion object {
        private val defaultRegistry: QuirkRegistry by lazy {
            QuirkRegistry(DeviceInfo.current()).also { registry ->
                ServiceLoader.load(QuirkProvider::class.java).forEach(registry::addProvider)
            }
        }

        public fun getInstance(): QuirkRegistry = defaultRegistry

        /** Create an isolated registry for testing. Providers are NOT auto-loaded. */
        public fun createForTest(device: DeviceInfo): QuirkRegistry = QuirkRegistry(device)
    }
}

@PublishedApi
internal class QuirkEntry<T : Any>(
    val key: QuirkKey<T>,
    val resolve: (DeviceInfo) -> T?,
)
