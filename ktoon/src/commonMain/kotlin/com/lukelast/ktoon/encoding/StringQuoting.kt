package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonEncodingException
import com.lukelast.ktoon.util.indexOfUnpairedSurrogate
import com.lukelast.ktoon.util.isAsciiDigit
import com.lukelast.ktoon.util.isAsciiLetter
import com.lukelast.ktoon.util.toCodePointLabel

/** Number of hex digits in a `\uXXXX` escape (§7.1). */
private const val UNICODE_ESCAPE_DIGITS = 4

private const val HEX_RADIX = 16

/** First character allowed to appear literally; C0 controls below it always require quoting. */
private const val FIRST_LITERAL_CHAR = 0x20

/** Size of the ASCII table, the range covered by the SPECIAL_CHARS lookup. */
private const val ASCII_TABLE_SIZE = 128

/** A leading U+FEFF is a document's byte-order mark, never content (§12). */
private const val BYTE_ORDER_MARK = '﻿'

/** First character of the §7.3 bare-key pattern `^[A-Za-z_][A-Za-z0-9_.]*$`. */
private fun Char.isBareKeyStart() = isAsciiLetter() || this == '_'

/** Every character after the first in the §7.3 bare-key pattern. */
private fun Char.isBareKeyPart() = isBareKeyStart() || isAsciiDigit() || this == '.'

/** §7.2: values spelled as one of these must be quoted so they do not decode as non-strings. */
private const val LITERAL_TRUE = "true"
private const val LITERAL_NULL = "null"
private const val LITERAL_FALSE = "false"

/** Utility for quoting and unquoting strings according to TOON format rules. */
internal object StringQuoting {

    enum class QuotingContext {
        OBJECT_KEY,
        OBJECT_VALUE,
        ARRAY_ELEMENT,

        /** A primitive that is the whole document, where the first character starts the file. */
        ROOT_VALUE,
    }

    // Lookup table for characters that ALWAYS require quoting (except delimiter which is dynamic)
    // Indices correspond to ASCII values.
    private val SPECIAL_CHARS = BooleanArray(ASCII_TABLE_SIZE)

    init {
        // Control characters, everything below the first character that may appear literally
        for (i in 0 until FIRST_LITERAL_CHAR) {
            SPECIAL_CHARS[i] = true
        }
        // Specific special characters
        SPECIAL_CHARS['"'.code] = true
        SPECIAL_CHARS['\\'.code] = true
        SPECIAL_CHARS[':'.code] = true
        SPECIAL_CHARS['['.code] = true
        SPECIAL_CHARS[']'.code] = true
        SPECIAL_CHARS['{'.code] = true
        SPECIAL_CHARS['}'.code] = true
        // Note: Delimiter is checked dynamically
    }

    /**
     * §3: host strings must be sequences of Unicode scalar values; an unpaired surrogate is not
     * representable in TOON and MUST error rather than be emitted or replaced.
     */
    private fun validateScalarValues(str: String) {
        val index = str.indexOfUnpairedSurrogate()
        if (index != -1) {
            throw KtoonEncodingException(
                "Unpaired surrogate ${str[index].toCodePointLabel()} cannot be encoded to TOON"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
    fun needsQuoting(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = KtoonConfiguration.Delimiter.COMMA.char,
    ): Boolean {
        validateScalarValues(str)
        if (str.isEmpty()) return true
        val len = str.length

        // Check first character
        val first = str[0]
        if (first == '-') return true // Starts with hyphen
        if (first == '#') return true // §7.2: would read as a comment line on decode
        // §12: a U+FEFF at the very start of a document is its byte-order mark, which encoders
        // must not emit; quoting keeps a value that begins with one out of that position.
        if (context == QuotingContext.ROOT_VALUE && first == BYTE_ORDER_MARK) return true
        if (first.code < ASCII_TABLE_SIZE && SPECIAL_CHARS[first.code])
            return true // Control or special

        // Check last character (trailing whitespace)
        // Leading whitespace is covered by control check (0-31 includes space? No, space is 32)
        // Spec says: "It has leading or trailing whitespace."
        if (first <= ' ') return true
        val last = str[len - 1]
        if (last <= ' ') return true

        // Check for specific keywords (§7.2 applies to values only; §7.3 keys quote purely by
        // the bare-key pattern, so a key spelled "true" stays bare)
        if (context != QuotingContext.OBJECT_KEY) {
            if (len == LITERAL_TRUE.length && (str == LITERAL_TRUE || str == LITERAL_NULL))
                return true
            if (len == LITERAL_FALSE.length && str == LITERAL_FALSE) return true
        }

        // Single pass loop
        var isNumericLike = true
        var hasInvalidKeyChar = false
        val checkKey = (context == QuotingContext.OBJECT_KEY)

        // Numeric state tracking
        var seenDot = false
        var seenExp = false
        var seenDigit = false

        for (i in 0 until len) {
            val c = str[i]
            val code = c.code

            // 1. Check special chars and delimiter
            if (code < ASCII_TABLE_SIZE) {
                if (SPECIAL_CHARS[code]) return true
            }
            // §11.1: the delimiter matters for every value position, not for keys.
            if (c == delimiter && context != QuotingContext.OBJECT_KEY) return true

            // 2. Check Key Validity (if needed)
            if (checkKey && !hasInvalidKeyChar) {
                val bare = if (i == 0) c.isBareKeyStart() else c.isBareKeyPart()
                if (!bare) hasInvalidKeyChar = true
            }

            // 3. Update Numeric State
            if (isNumericLike) {
                if (c.isAsciiDigit()) {
                    seenDigit = true
                } else if (c == '.') {
                    // §7.2's regex needs a digit before the dot (".5" stays bare) and at least
                    // one after it before any exponent ("1.e5" stays bare)
                    if (seenDot || seenExp || !seenDigit) isNumericLike = false
                    seenDot = true
                    seenDigit = false
                } else if (c == 'e' || c == 'E') {
                    if (seenExp || !seenDigit) isNumericLike = false
                    seenExp = true
                    seenDigit = false // Need digits after E
                } else if (c == '+' || c == '-') {
                    // §7.2: a leading sign keeps the token numeric-like ("+1" must be quoted);
                    // otherwise a sign is only allowed directly after the exponent marker.
                    val leadingSign = i == 0
                    val afterExponentMarker =
                        !leadingSign && (str[i - 1] == 'e' || str[i - 1] == 'E')
                    if (!leadingSign && !(seenExp && afterExponentMarker)) {
                        isNumericLike = false
                    }
                } else {
                    isNumericLike = false
                }
            }
        }

        if (checkKey && hasInvalidKeyChar) return true

        // Final numeric check
        // Must end with digit if it's a number?
        // Spec: "Matches /^-?\d+(?:\.\d+)?(?:e[+-]?\d+)?$/i"
        // This implies it must end with a digit.
        if (isNumericLike) {
            if (last.isAsciiDigit()) {
                return true
            }
        }

        return false
    }

    fun quote(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = KtoonConfiguration.Delimiter.COMMA.char,
    ): String {
        if (!needsQuoting(str, context, delimiter)) return str
        val len = str.length
        val sb = StringBuilder(len + 2)
        sb.append('"')
        for (i in str.indices) {
            when (val c = str[i]) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else ->
                    // §7.1: control characters outside \n, \r, \t are emitted as \uXXXX
                    if (c.code < FIRST_LITERAL_CHAR) {
                        sb.append("\\u")
                        sb.append(c.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_DIGITS, '0'))
                    } else {
                        sb.append(c)
                    }
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
