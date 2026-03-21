package com.atruedev.kmpble.quirks

/**
 * SPI for contributing device quirks. Discovered at runtime via [java.util.ServiceLoader].
 * Implementations populate the [QuirkRegistry] with typed quirk entries.
 */
public interface QuirkProvider {
    public fun contribute(registry: QuirkRegistry)
}
