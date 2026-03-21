package com.atruedev.kmpble.quirks

/**
 * Typed identifier for a device quirk.
 * Well-known keys are defined in [BleQuirks]. Uses reference equality for matching.
 *
 * @param describe formats a resolved value for diagnostics; return `null` to omit from [QuirkRegistry.describe].
 */
public class QuirkKey<T : Any>(
    public val name: String,
    public val default: T,
    internal val describe: (T) -> String? = { null },
) {
    override fun toString(): String = "QuirkKey($name)"
}
