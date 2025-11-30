package com.lukelast.ktoon.encoding

import java.math.BigDecimal

/**
 * Normalizes numbers to TOON canonical format:
 * no scientific notation, no trailing zeros, NaN/Infinity become null.
 */
internal object NumberNormalizer {

    fun normalize(value: Double): String = when {
        value.isNaN() || value.isInfinite() -> "null"
        value == 0.0 || value == -0.0 -> "0"
        else -> BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
            .let { if (it == "-0" || it == "-0.0") "0" else it }
    }

    fun normalize(value: Float): String = when {
        value.isNaN() || value.isInfinite() -> "null"
        value == 0.0f || value == -0.0f -> "0"
        else -> normalize(value.toDouble())
    }

    fun normalize(value: Long): String = value.toString()
    fun normalize(value: Int): String = value.toString()
    fun normalize(value: Short): String = value.toString()
    fun normalize(value: Byte): String = value.toString()

}
