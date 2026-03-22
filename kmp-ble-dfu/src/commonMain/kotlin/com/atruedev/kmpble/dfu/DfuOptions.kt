package com.atruedev.kmpble.dfu

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class DfuOptions(
    val prnInterval: Int = 10,
    val retryCount: Int = 3,
    val retryDelay: Duration = 2.seconds,
    val commandTimeout: Duration = 10.seconds,
    val useL2cap: Boolean = false,
    val l2capPsm: Int? = null,
)
