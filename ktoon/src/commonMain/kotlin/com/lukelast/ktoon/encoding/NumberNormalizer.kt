package com.lukelast.ktoon.encoding

/**
 * Normalizes numbers to TOON canonical format per SPEC §2:
 * - No exponent notation (e.g., 1e6 → 1000000, 1e-6 → 0.000001)
 * - No trailing zeros in fractional part (e.g., 1.5000 → 1.5)
 * - If fractional part is zero, emit as integer (e.g., 1.0 → 1)
 * - -0 normalized to 0
 * - NaN/Infinity become null
 */
internal object NumberNormalizer {

    fun normalize(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0) return "0" // Handles both 0.0 and -0.0

        val longValue = value.toLong()
        if (value == longValue.toDouble()) {
            return longValue.toString()
        }

        return normalizeDecimalString(value.toString())
    }

    fun normalize(value: Float): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0f) return "0" // Handles both 0.0f and -0.0f

        val longValue = value.toLong()
        if (value == longValue.toFloat()) {
            return longValue.toString()
        }

        return normalizeDecimalString(value.toString())
    }

    /**
     * Normalizes a numeric string: expands scientific notation and strips trailing zeros.
     */
    private fun normalizeDecimalString(s: String): String {
        val eIndex = s.indexOfFirst { it == 'E' || it == 'e' }

        return if (eIndex == -1) {
            stripTrailingZeros(s)
        } else {
            val mantissa = s.substring(0, eIndex)
            val exponent = s.substring(eIndex + 1).toInt()
            stripTrailingZeros(expandScientificNotation(mantissa, exponent))
        }
    }

    /**
     * Expands scientific notation to plain decimal form.
     * E.g., "1.5", 10 → "15000000000"; "1", -6 → "0.000001"
     */
    private fun expandScientificNotation(mantissa: String, exponent: Int): String {
        val negative = mantissa.startsWith('-')
        val absM = if (negative) mantissa.substring(1) else mantissa

        // Split mantissa into integer and fractional parts
        val dotIndex = absM.indexOf('.')
        val intPart: String
        val fracPart: String
        if (dotIndex >= 0) {
            intPart = absM.substring(0, dotIndex)
            fracPart = absM.substring(dotIndex + 1)
        } else {
            intPart = absM
            fracPart = ""
        }

        // Combine all digits; decimal position is after intPart.length
        val allDigits = intPart + fracPart
        val currentDecimalPos = intPart.length
        val newDecimalPos = currentDecimalPos + exponent

        val result = when {
            newDecimalPos <= 0 -> {
                // Need leading zeros: 0.000...digits
                "0." + "0".repeat(-newDecimalPos) + allDigits
            }
            newDecimalPos >= allDigits.length -> {
                // Whole number, possibly with trailing zeros
                allDigits + "0".repeat(newDecimalPos - allDigits.length)
            }
            else -> {
                // Decimal point in the middle
                allDigits.substring(0, newDecimalPos) + "." + allDigits.substring(newDecimalPos)
            }
        }

        return if (negative) "-$result" else result
    }

    /**
     * Strips trailing zeros from fractional part and removes decimal point if no fraction remains.
     */
    private fun stripTrailingZeros(s: String): String {
        if (!s.contains('.')) return s
        return s.trimEnd('0').trimEnd('.')
    }

    fun normalize(value: Long): String = value.toString()

    fun normalize(value: Int): String = value.toString()

    fun normalize(value: Short): String = value.toString()

    fun normalize(value: Byte): String = value.toString()
}
