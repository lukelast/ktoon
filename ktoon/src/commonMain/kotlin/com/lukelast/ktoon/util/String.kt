package com.lukelast.ktoon.util

/** Bit that separates ASCII upper case from lower case: `'A'.code or 0x20 == 'a'.code`. */
private const val ASCII_CASE_BIT = 0x20

private const val ALPHABET_LETTERS = 26u

private const val DECIMAL_DIGITS = 10u

fun Char.isAlpha(): Boolean {
    // 1. (c.code or ASCII_CASE_BIT): Force the char to lowercase (e.g., 'A' becomes 'a')
    // 2. Subtract 'a': Align the range to start at 0
    return ((code or ASCII_CASE_BIT) - 'a'.code).toUInt() < ALPHABET_LETTERS
}

fun Char.isDigit(): Boolean {
    // Subtracts '0'. If c was less than '0', it wraps around to a huge
    // positive number (because of UInt). If it's 0-9, it stays small.
    return (this - '0').toUInt() < DECIMAL_DIGITS
}
