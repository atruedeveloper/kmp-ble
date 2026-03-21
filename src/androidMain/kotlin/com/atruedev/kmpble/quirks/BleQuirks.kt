package com.atruedev.kmpble.quirks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Well-known quirk keys used by the BLE connection logic. */
public object BleQuirks {

    /** Samsung: some Galaxy devices fail to connect unless already bonded. */
    public val BondBeforeConnect: QuirkKey<Boolean> =
        QuirkKey("bondBeforeConnect", false) { if (it) "bond-before-connect" else null }

    /** Pixel: GATT 133 on first attempt — retry after this delay. */
    public val GattRetryDelay: QuirkKey<Duration> =
        QuirkKey("gattRetryDelay", 300.milliseconds) { if (it != 300.milliseconds) "retry-delay=$it" else null }

    /** Number of times to retry `connectGatt()` on failure. */
    public val GattRetryCount: QuirkKey<Int> =
        QuirkKey("gattRetryCount", 1) { if (it > 1) "retry=${it}x" else null }

    /** OnePlus/Xiaomi: cache stale services after bonding — refresh required. */
    public val RefreshServicesOnBond: QuirkKey<Boolean> =
        QuirkKey("refreshServicesOnBond", false) { if (it) "refresh-services-on-bond" else null }

    /** Xiaomi: delays bond state callbacks significantly. */
    public val BondStateTimeout: QuirkKey<Duration> =
        QuirkKey("bondStateTimeout", 10.seconds) { if (it != 10.seconds) "bond-timeout=$it" else null }

    /** Some OEMs need longer connection timeouts. */
    public val ConnectionTimeout: QuirkKey<Duration> =
        QuirkKey("connectionTimeout", 30.seconds) { if (it != 30.seconds) "conn-timeout=$it" else null }
}
