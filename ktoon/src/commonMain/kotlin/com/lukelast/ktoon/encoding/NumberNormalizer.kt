package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.util.isAsciiDigit

/** §2: plain decimal form is required down to 1e-6, i.e. a decimal point at position -5. */
private const val PLAIN_MIN_POINT = -5

/** §2: plain decimal form is required up to but excluding 1e21. */
private const val PLAIN_MAX_POINT = 21

/**
 * Normalizes numbers to TOON canonical format per SPEC §2:
 * - No exponent notation (e.g., 1e6 → 1000000, 1e-6 → 0.000001)
 * - No trailing zeros in fractional part (e.g., 1.5000 → 1.5)
 * - If fractional part is zero, emit as integer (e.g., 1.0 → 1)
 * - -0 normalized to 0
 * - NaN/Infinity become null
 */
@Suppress("TooManyFunctions")
internal object NumberNormalizer {

    @Suppress("ReturnCount")
    fun normalize(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0) return "0" // Handles both 0.0 and -0.0

        wholeNumberStringOrNull(value)?.let {
            return it
        }

        return normalizeDecimalString(value.toString())
    }

    @Suppress("ReturnCount")
    fun normalize(value: Float): String {
        if (value.isNaN() || value.isInfinite()) return "null"
        if (value == 0.0f) return "0" // Handles both 0.0f and -0.0f

        // Every Float widens to Double exactly, so the shared probe sees the same value.
        wholeNumberStringOrNull(value.toDouble())?.let {
            return it
        }

        return normalizeDecimalString(value.toString())
    }

    /**
     * Renders [value] as an exact integer, or null when it is not integral. `toLong` saturates, so
     * a result of `Long.MAX_VALUE` is ambiguous: it is either that value or 2^63, which converts
     * back to the same Double. The unsigned re-check tells the two apart (§2: an encoder must emit
     * enough precision for the value to decode back unchanged).
     */
    @Suppress("ReturnCount")
    private fun wholeNumberStringOrNull(value: Double): String? {
        val signed = value.toLong()
        if (value != signed.toDouble()) return null
        if (signed != Long.MAX_VALUE) return signed.toString()

        val unsigned = value.toULong()
        return if (value == unsigned.toDouble()) unsigned.toString() else null
    }

    /** Normalizes a numeric string: expands scientific notation and strips trailing zeros. */
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
     * Expands scientific notation to plain decimal form. E.g., "1.5", 10 → "15000000000"; "1", -6 →
     * "0.000001"
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

        val result =
            when {
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

    /**
     * Canonicalizes an already-validated numeric literal without routing it through a host number,
     * so digits a `Long` or `Double` cannot hold survive. §2: plain decimal form is required for 0
     * and for 1e-6 ≤ |n| < 1e21; outside that range exponent notation is allowed, and is used here
     * so a literal like `1e400` stays short.
     */
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun normalizeLiteral(literal: String): String {
        var i = 0
        val negative = literal[0] == '-'
        if (negative) i++

        val intStart = i
        while (i < literal.length && literal[i].isAsciiDigit()) i++
        val intPart = literal.substring(intStart, i)

        var fracPart = ""
        if (i < literal.length && literal[i] == '.') {
            i++
            val fracStart = i
            while (i < literal.length && literal[i].isAsciiDigit()) i++
            fracPart = literal.substring(fracStart, i)
        }

        var exponent = 0
        if (i < literal.length && (literal[i] == 'e' || literal[i] == 'E')) {
            // An exponent beyond Int is beyond any representable magnitude; keep the source text.
            exponent = literal.substring(i + 1).toIntOrNull() ?: return literal
        }

        // The digits with the decimal point at `pointPos`, so the value is 0.<digits> *
        // 10^pointPos.
        val digits = intPart + fracPart
        var lead = 0
        while (lead < digits.length && digits[lead] == '0') lead++
        val significant = digits.substring(lead).trimEnd('0')
        if (significant.isEmpty()) return "0" // §2: -0 normalizes to 0
        val pointPos = intPart.length + exponent - lead

        val body =
            if (pointPos in PLAIN_MIN_POINT..PLAIN_MAX_POINT) {
                plainDecimal(significant, pointPos)
            } else {
                exponentForm(significant, pointPos)
            }
        return if (negative) "-$body" else body
    }

    private fun plainDecimal(significant: String, pointPos: Int): String =
        when {
            pointPos <= 0 -> "0." + "0".repeat(-pointPos) + significant
            pointPos >= significant.length ->
                significant + "0".repeat(pointPos - significant.length)
            else -> significant.substring(0, pointPos) + "." + significant.substring(pointPos)
        }

    private fun exponentForm(significant: String, pointPos: Int): String {
        val mantissa =
            if (significant.length == 1) significant
            else significant.substring(0, 1) + "." + significant.substring(1)
        val exponent = pointPos - 1
        return if (exponent < 0) "${mantissa}e$exponent" else "${mantissa}e+$exponent"
    }

    fun normalize(value: Long): String = value.toString()

    fun normalize(value: Int): String = value.toString()

    fun normalize(value: Short): String = value.toString()

    fun normalize(value: Byte): String = value.toString()
}
