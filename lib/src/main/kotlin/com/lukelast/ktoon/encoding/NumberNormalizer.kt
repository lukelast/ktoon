package com.lukelast.ktoon.encoding

import java.math.BigDecimal

/**
 * Normalizes numbers to TOON canonical format: no scientific notation, no trailing zeros,
 * NaN/Infinity become null.
 */
internal object NumberNormalizer {

    fun normalize(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0) return "0"

        val longValue = value.toLong()
        if (value == longValue.toDouble()) {
            return longValue.toString()
        }

        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().let {
            if (it == "-0" || it == "-0.0") "0" else it
        }
    }

    fun normalize(value: Float): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0f) return "0"

        val longValue = value.toLong()
        if (value == longValue.toFloat()) {
            return longValue.toString()
        }

        return value.toBigDecimal().stripTrailingZeros().toPlainString().let {
            if (it == "-0" || it == "-0.0") "0" else it
        }
    }

    fun normalize(value: Long): String = value.toString()

    fun normalize(value: Int): String = value.toString()

    fun normalize(value: Short): String = value.toString()

    fun normalize(value: Byte): String = value.toString()
}
