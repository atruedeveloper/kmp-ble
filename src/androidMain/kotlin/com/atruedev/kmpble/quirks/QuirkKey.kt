package com.atruedev.kmpble.quirks

/**
 * Typed identifier for a device quirk.
 * Well-known keys are defined in [BleQuirks]. Uses reference equality for matching.
 */
public class QuirkKey<T : Any>(
    public val name: String,
    public val default: T,
) {
    override fun toString(): String = "QuirkKey($name)"
}
