package com.atruedev.kmpble.quirks

import java.util.ServiceLoader

/**
 * Immutable registry that resolves device-specific BLE quirks.
 *
 * Resolution priority:
 * 1. User overrides (registered via [configure])
 * 2. Provider entries (contributed via [QuirkProvider])
 * 3. [QuirkKey.default]
 *
 * The registry is frozen after construction — all mutation happens through [Builder].
 */
public class QuirkRegistry @PublishedApi internal constructor(
    @PublishedApi internal val device: DeviceInfo,
    @PublishedApi internal val entries: List<QuirkEntry<*>>,
) {

    public inline fun <reified T : Any> resolve(key: QuirkKey<T>): T {
        for (entry in entries) {
            if (entry.key !== key) continue
            val result = entry.resolve(device) as? T ?: continue
            return result
        }
        return key.default
    }

    /** Human-readable summary of active quirks for connection-time logging. */
    public fun describe(): String {
        val seen = mutableSetOf<QuirkKey<*>>()
        val active = buildList {
            for (entry in entries) {
                if (entry.key in seen) continue
                val (matched, desc) = entry.tryDescribe(device)
                if (!matched) continue
                seen.add(entry.key)
                if (desc != null) add(desc)
            }
        }
        val suffix = if (active.isEmpty()) "no device-specific quirks" else active.joinToString()
        return "${device.manufacturer}/${device.model} — $suffix"
    }

    /** Mutable builder for constructing a [QuirkRegistry]. All entries are appended in registration order. */
    public class Builder internal constructor(private val device: DeviceInfo) {
        private val entries = mutableListOf<QuirkEntry<*>>()

        /** Register a quirk for devices matching [match]. */
        public fun <T : Any> register(key: QuirkKey<T>, value: T, match: (DeviceInfo) -> Boolean) {
            entries.add(QuirkEntry(key) { d -> if (match(d)) value else null })
        }

        /** Register a quirk using a hierarchical device key (e.g. `"manufacturer:model"`). */
        public fun <T : Any> register(key: QuirkKey<T>, value: T, deviceKey: String) {
            register(key, value) { DeviceMatch.matchesAny(it, setOf(deviceKey)) }
        }

        /** Register a quirk with a device-key-to-value map. Uses hierarchical matching. */
        public fun <T : Any> register(key: QuirkKey<T>, entries: Map<String, T>) {
            this.entries.add(QuirkEntry(key) { device -> DeviceMatch.matchFirst(device, entries) })
        }

        public fun addProvider(provider: QuirkProvider) {
            provider.contribute(this)
        }

        /** Later registrations take priority (last-write-wins). */
        @PublishedApi
        internal fun build(): QuirkRegistry = QuirkRegistry(device, entries.asReversed().toList())
    }

    public companion object {
        private var userConfig: (Builder.() -> Unit)? = null

        /**
         * Configure custom quirks before first [getInstance] call.
         * Must be called during app initialization (e.g., `Application.onCreate`).
         */
        public fun configure(block: Builder.() -> Unit) {
            userConfig = block
        }

        private val defaultRegistry: QuirkRegistry by lazy {
            Builder(DeviceInfo.current()).apply {
                ServiceLoader.load(QuirkProvider::class.java).forEach(::addProvider)
                userConfig?.invoke(this)
            }.build()
        }

        public fun getInstance(): QuirkRegistry = defaultRegistry

        /** Create an isolated registry for testing. Providers are NOT auto-loaded. */
        public fun createForTest(device: DeviceInfo, block: Builder.() -> Unit = {}): QuirkRegistry =
            Builder(device).apply(block).build()
    }
}

@PublishedApi
internal class QuirkEntry<T : Any>(
    val key: QuirkKey<T>,
    private val resolver: (DeviceInfo) -> T?,
) {
    fun resolve(device: DeviceInfo): T? = resolver(device)

    fun tryDescribe(device: DeviceInfo): Pair<Boolean, String?> {
        val value = resolver(device) ?: return false to null
        return true to key.describe(value)
    }
}
