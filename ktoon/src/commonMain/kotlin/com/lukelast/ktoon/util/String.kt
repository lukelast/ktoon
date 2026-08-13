package com.lukelast.ktoon.util

/** Bit that separates ASCII upper case from lower case: `'A'.code or 0x20 == 'a'.code`. */
private const val ASCII_CASE_BIT = 0x20

private const val ALPHABET_LETTERS = 26u

private const val DECIMAL_DIGITS = 10u

private const val HEX_RADIX = 16

/** Number of hex digits in a `U+XXXX` label (§7.1). */
private const val UNICODE_ESCAPE_DIGITS = 4

internal fun Char.isAsciiLetter(): Boolean {
    // 1. (c.code or ASCII_CASE_BIT): Force the char to lowercase (e.g., 'A' becomes 'a')
    // 2. Subtract 'a': Align the range to start at 0
    return ((code or ASCII_CASE_BIT) - 'a'.code).toUInt() < ALPHABET_LETTERS
}

internal fun Char.isAsciiDigit(): Boolean {
    // Subtracts '0'. If c was less than '0', it wraps around to a huge
    // positive number (because of UInt). If it's 0-9, it stays small.
    return (this - '0').toUInt() < DECIMAL_DIGITS
}

/** Renders a character as its `U+XXXX` code point label. */
internal fun Char.toCodePointLabel(): String =
    "U+" + code.toString(HEX_RADIX).uppercase().padStart(UNICODE_ESCAPE_DIGITS, '0')

/**
 * §3/§7.1: TOON text is a sequence of Unicode scalar values, so a surrogate may only appear as part
 * of a well-formed pair. Returns the index of the first unpaired surrogate, or -1 if there is none.
 */
internal fun String.indexOfUnpairedSurrogate(): Int {
    for (i in indices) {
        val c = this[i]
        val paired =
            when {
                c.isHighSurrogate() -> i + 1 < length && this[i + 1].isLowSurrogate()
                c.isLowSurrogate() -> i > 0 && this[i - 1].isHighSurrogate()
                else -> true
            }
        if (!paired) return i
    }
    return -1
}
