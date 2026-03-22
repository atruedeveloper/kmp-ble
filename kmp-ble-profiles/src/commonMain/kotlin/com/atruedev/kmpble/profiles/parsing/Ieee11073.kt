package com.atruedev.kmpble.profiles.parsing

import kotlin.math.pow

private const val SFLOAT_NAN = 0x07FF
private const val SFLOAT_NRES = 0x0800
private const val SFLOAT_POS_INFINITY = 0x07FE
private const val SFLOAT_NEG_INFINITY = 0x0802
private const val SFLOAT_RESERVED = 0x0801

private const val FLOAT_NAN = 0x007FFFFF
private const val FLOAT_NRES = 0x00800000
private const val FLOAT_POS_INFINITY = 0x007FFFFE
private const val FLOAT_NEG_INFINITY = 0x00800002
private const val FLOAT_RESERVED = 0x00800001

/**
 * Converts a 16-bit IEEE 11073-20601 SFLOAT to a [Float].
 *
 * Returns `null` for special values (NaN, NRes, +/-Infinity, Reserved).
 *
 * @param raw unsigned 16-bit value read from BLE characteristic
 */
public fun sfloatToFloat(raw: Int): Float? {
    val mantissa = raw and 0x0FFF
    val exponent = (raw shr 12) and 0x0F

    if (mantissa == SFLOAT_NAN || mantissa == SFLOAT_NRES ||
        mantissa == SFLOAT_POS_INFINITY || mantissa == SFLOAT_NEG_INFINITY ||
        mantissa == SFLOAT_RESERVED
    ) return null

    val signedMantissa = if (mantissa >= 0x0800) mantissa - 0x1000 else mantissa
    val signedExponent = if (exponent >= 0x08) exponent - 0x10 else exponent

    return (signedMantissa * 10.0.pow(signedExponent)).toFloat()
}

/**
 * Converts a 32-bit IEEE 11073-20601 FLOAT to a [Float].
 *
 * Returns `null` for special values (NaN, NRes, +/-Infinity, Reserved).
 *
 * @param raw unsigned 32-bit value read from BLE characteristic
 */
public fun floatToFloat(raw: Long): Float? {
    val mantissa = (raw and 0x00FFFFFF).toInt()
    val exponent = ((raw shr 24) and 0xFF).toInt()

    if (mantissa == FLOAT_NAN || mantissa == FLOAT_NRES ||
        mantissa == FLOAT_POS_INFINITY || mantissa == FLOAT_NEG_INFINITY ||
        mantissa == FLOAT_RESERVED
    ) return null

    val signedMantissa = if (mantissa >= 0x800000) mantissa - 0x1000000 else mantissa
    val signedExponent = if (exponent >= 0x80) exponent - 0x100 else exponent

    return (signedMantissa * 10.0.pow(signedExponent)).toFloat()
}
