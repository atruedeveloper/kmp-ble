package com.atruedev.kmpble.quirks

/**
 * Hierarchical device matching for quirk resolution.
 *
 * Keys use colon-separated segments: `"manufacturer"`, `"manufacturer:model"`,
 * `"manufacturer:model:display"`. Matching tries most specific first.
 */
public object DeviceMatch {

    /** Captures Samsung SM-XXXX series prefixes (e.g. "sm-g99" for Galaxy S21 series). */
    public const val MODEL_PREFIX_LENGTH: Int = 6

    public fun generateMatchKeys(device: DeviceInfo): List<String> = listOf(
        "${device.manufacturer}:${device.model}:${device.display}",
        "${device.manufacturer}:${device.model}",
        "${device.manufacturer}:${device.model.take(MODEL_PREFIX_LENGTH)}",
        device.manufacturer,
    )

    public fun matchesAny(device: DeviceInfo, entries: Set<String>): Boolean =
        generateMatchKeys(device).any { it in entries }

    public fun <T> matchFirst(device: DeviceInfo, entries: Map<String, T>): T? =
        generateMatchKeys(device).firstNotNullOfOrNull { entries[it] }
}
