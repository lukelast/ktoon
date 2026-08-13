package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.util.isAsciiDigit

/** Number of decimal digits in the widest `Long`; anything longer cannot be one. */
private const val MAX_LONG_DIGITS = 19

/**
 * §4 number grammar: an unquoted token decodes as a number iff it matches
 * `/^-?[0-9]+(?:\.[0-9]+)?(?:e[+-]?[0-9]+)?$/i` (ASCII digits only) without forbidden leading
 * zeros. Anything else — `.5`, `1.`, `+5`, `Infinity`, `NaN`, `0x10`, `1_000` — is a string, and
 * this decision MUST NOT be delegated to a wider host parser.
 */
@Suppress("CyclomaticComplexMethod", "ReturnCount")
internal fun matchesNumberGrammar(str: String): Boolean {
    var i = 0
    if (i < str.length && str[i] == '-') i++

    val intStart = i
    while (i < str.length && str[i].isAsciiDigit()) i++
    val intLen = i - intStart
    if (intLen == 0) return false
    // Forbidden leading zeros in the integer part (e.g. "05", "-0001")
    if (intLen > 1 && str[intStart] == '0') return false

    if (i < str.length && str[i] == '.') {
        i++
        val fracStart = i
        while (i < str.length && str[i].isAsciiDigit()) i++
        if (i == fracStart) return false
    }

    if (i < str.length && (str[i] == 'e' || str[i] == 'E')) {
        i++
        if (i < str.length && (str[i] == '+' || str[i] == '-')) i++
        val expStart = i
        while (i < str.length && str[i].isAsciiDigit()) i++
        if (i == expStart) return false
    }

    return i == str.length
}

/**
 * The exact value of a [matchesNumberGrammar]-valid literal as a `Long`, or null when the literal
 * is not a whole number or does not fit. The digits are examined directly: routing through `Double`
 * first would round `9007199254740993` and would make `9223372036854775808` indistinguishable from
 * `Long.MAX_VALUE` (§4).
 */
@Suppress("ReturnCount", "CyclomaticComplexMethod")
internal fun exactIntegralValue(str: String): Long? {
    var i = 0
    val negative = str[0] == '-'
    if (negative) i++

    val intStart = i
    while (i < str.length && str[i].isAsciiDigit()) i++
    val intPart = str.substring(intStart, i)

    var fracPart = ""
    if (i < str.length && str[i] == '.') {
        i++
        val fracStart = i
        while (i < str.length && str[i].isAsciiDigit()) i++
        fracPart = str.substring(fracStart, i)
    }

    var exponent = 0
    if (i < str.length && (str[i] == 'e' || str[i] == 'E')) {
        // An exponent too large for Int is far outside Long either way.
        exponent = str.substring(i + 1).toIntOrNull() ?: return null
    }

    // Digits with the decimal point at `pointPos`, leading zeros dropped so the position reflects
    // the value's real magnitude.
    val digits = intPart + fracPart
    var lead = 0
    while (lead < digits.length && digits[lead] == '0') lead++
    val significant = digits.substring(lead)
    if (significant.isEmpty()) return 0L
    val pointPos = intPart.length + exponent - lead

    // Every digit after the point must be zero for the value to be a whole number.
    for (j in maxOf(pointPos, 0) until significant.length) {
        if (significant[j] != '0') return null
    }
    if (pointPos <= 0) return null
    if (pointPos > MAX_LONG_DIGITS) return null

    val whole =
        if (pointPos <= significant.length) significant.substring(0, pointPos)
        else significant + "0".repeat(pointPos - significant.length)
    return (if (negative) "-$whole" else whole).toLongOrNull()
}
