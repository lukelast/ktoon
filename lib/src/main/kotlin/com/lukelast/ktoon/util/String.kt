package com.lukelast.ktoon.util

/** Checks if a string is a valid IdentifierSegment per §1.9: matching `^[A-Za-z_][A-Za-z0-9_]*$` */
fun String.isIdentifierSegment(): Boolean {
    if (isEmpty()) return false
    val first = this[0]
    if (!first.isAlpha() && first != '_') return false
    for (i in 1 until length) {
        val c = this[i]
        if (!c.isAlpha() && !c.isDigit() && c != '_') return false
    }
    return true
}

fun Char.isAlpha(): Boolean {
    // 1. (c.code or 0x20): Force the char to lowercase (e.g., 'A' becomes 'a')
    // 2. Subtract 'a': Align the range to start at 0
    // 3. Check if result is < 26 (the number of letters in alphabet)
    return ((code or 0x20) - 'a'.code).toUInt() < 26u
}

fun Char.isDigit(): Boolean {
    // Subtracts '0'. If c was less than '0', it wraps around to a huge
    // positive number (because of UInt). If it's 0-9, it stays small.
    return (this - '0').toUInt() < 10u
}
