package com.atruedev.kmpble.quirks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Well-known quirk keys used by the BLE connection logic.
 *
 * Each key identifies a device-specific behavior with a sensible default.
 * Values are resolved at connection time via [QuirkRegistry].
 */
public object BleQuirks {

    /** Samsung: some Galaxy devices fail to connect unless already bonded. */
    public val BondBeforeConnect: QuirkKey<Boolean> = QuirkKey("bondBeforeConnect", false)

    /** Pixel: GATT 133 on first attempt — retry after this delay. */
    public val GattRetryDelay: QuirkKey<Duration> = QuirkKey("gattRetryDelay", 300.milliseconds)

    /** Number of times to retry `connectGatt()` on failure. */
    public val GattRetryCount: QuirkKey<Int> = QuirkKey("gattRetryCount", 1)

    /** OnePlus/Xiaomi: cache stale services after bonding — refresh required. */
    public val RefreshServicesOnBond: QuirkKey<Boolean> = QuirkKey("refreshServicesOnBond", false)

    /** Xiaomi: delays bond state callbacks significantly. */
    public val BondStateTimeout: QuirkKey<Duration> = QuirkKey("bondStateTimeout", 10.seconds)

    /** Some OEMs need longer connection timeouts. */
    public val ConnectionTimeout: QuirkKey<Duration> = QuirkKey("connectionTimeout", 30.seconds)
}
