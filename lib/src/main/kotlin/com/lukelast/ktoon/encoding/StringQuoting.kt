package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.util.isAlpha
import com.lukelast.ktoon.util.isDigit

/** Utility for quoting and unquoting strings according to TOON format rules. */
internal object StringQuoting {

    enum class QuotingContext {
        OBJECT_KEY,
        OBJECT_VALUE,
        ARRAY_ELEMENT,
    }

    // Lookup table for characters that ALWAYS require quoting (except delimiter which is dynamic)
    // Indices correspond to ASCII values.
    private val SPECIAL_CHARS = BooleanArray(128)

    init {
        // Control characters (0-31)
        for (i in 0..31) {
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

    fun needsQuoting(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = KtoonConfiguration.Delimiter.COMMA.char,
    ): Boolean {
        if (str.isEmpty()) return true
        val len = str.length

        // Check first character
        val first = str[0]
        if (first == '-') return true // Starts with hyphen
        if (first.code < 128 && SPECIAL_CHARS[first.code]) return true // Control or special

        // Check last character (trailing whitespace)
        // Leading whitespace is covered by control check (0-31 includes space? No, space is 32)
        // Spec says: "It has leading or trailing whitespace."
        if (first <= ' ') return true
        val last = str[len - 1]
        if (last <= ' ') return true

        // Check for specific keywords
        if (len == 4 && (str == "true" || str == "null")) return true
        if (len == 5 && str == "false") return true

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
            if (code < 128) {
                if (SPECIAL_CHARS[code]) return true
            }
            if (c == delimiter) {
                if (
                    context == QuotingContext.ARRAY_ELEMENT ||
                        context == QuotingContext.OBJECT_VALUE
                ) {
                    return true
                }
            }

            // 2. Check Key Validity (if needed)
            if (checkKey && !hasInvalidKeyChar) {
                // ^[A-Za-z_][A-Za-z0-9_.]*$
                if (i == 0) {
                    if (!c.isAlpha() && c != '_') hasInvalidKeyChar = true
                } else {
                    if (!c.isAlpha() && !c.isDigit() && c != '_' && c != '.')
                        hasInvalidKeyChar = true
                }
            }

            // 3. Update Numeric State
            if (isNumericLike) {
                if (c.isDigit()) {
                    seenDigit = true
                } else if (c == '.') {
                    if (seenDot || seenExp) isNumericLike = false
                    seenDot = true
                } else if (c == 'e' || c == 'E') {
                    if (seenExp || !seenDigit) isNumericLike = false
                    seenExp = true
                    seenDigit = false // Need digits after E
                } else if (c == '+' || c == '-') {
                    // Sign only allowed at start (handled) or after E
                    if (!seenExp || (str[i - 1] != 'e' && str[i - 1] != 'E')) isNumericLike = false
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
            if (last.isDigit()) {
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
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
