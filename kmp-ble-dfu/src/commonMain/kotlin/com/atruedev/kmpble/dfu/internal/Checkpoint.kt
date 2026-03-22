package com.atruedev.kmpble.dfu.internal

internal data class Checkpoint(
    val objectType: Int,
    val offset: Int,
    val crc32: UInt,
)
