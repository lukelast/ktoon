package com.lukelast.ktoon.util

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
