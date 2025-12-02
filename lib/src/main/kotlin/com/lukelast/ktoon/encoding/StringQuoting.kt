package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.util.isAlpha

/** Utility for quoting and unquoting strings according to TOON format rules. */
internal object StringQuoting {

    enum class QuotingContext {
        OBJECT_KEY,
        OBJECT_VALUE,
        ARRAY_ELEMENT,
    }

    fun needsQuoting(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = KtoonConfiguration.Delimiter.COMMA.char,
    ): Boolean {
        if (str.isEmpty()) return true
        val len = str.length
        if (len == 4 && (str == "true" || str == "null")) {
            return true
        } else if (len == 5 && str == "false") {
            return true
        }

        val first = str[0]
        if (first == '-') return true
        if (first <= ' ') return true // Starts with whitespace or control

        val last = str[len - 1]
        if (last <= ' ') return true // Ends with whitespace or control

        if (isNumber(str)) return true
        val contextIsArrayOrObject =
            (context == QuotingContext.OBJECT_VALUE || context == QuotingContext.ARRAY_ELEMENT)

        for (i in str.indices) {
            val c = str[i]
            if (c < ' ') return true // Control char
            if (shouldQuoteChar(c)) return true
            if (contextIsArrayOrObject && c == delimiter) {
                return true
            }
        }

        if (context == QuotingContext.OBJECT_KEY) {
            if (!isValidUnquotedKey(str)) return true
        }

        return false
    }

    private fun shouldQuoteChar(c: Char): Boolean {
        return c == '"' ||
            c == '\\' ||
            c == '\n' ||
            c == '\r' ||
            c == '\t' ||
            c == ':' ||
            c == '[' ||
            c == ']' ||
            c == '{' ||
            c == '}'
    }

    private fun isNumber(str: String): Boolean {
        if (str.isEmpty()) return false
        var i = 0
        val len = str.length
        if (str[i] == '-') {
            i++
            if (i == len) return false
        }

        var hasDot = false
        var hasExp = false
        var hasDigit = false

        while (i < len) {
            val c = str[i]
            if (c.isDigit()) {
                hasDigit = true
            } else if (c == '.') {
                if (hasDot || hasExp) return false
                hasDot = true
            } else if (c == 'e' || c == 'E') {
                if (hasExp || !hasDigit) return false
                hasExp = true
                if (i + 1 < len && (str[i + 1] == '+' || str[i + 1] == '-')) {
                    i++
                }
                if (i + 1 == len) return false
                hasDigit = false
            } else {
                return false
            }
            i++
        }
        return hasDigit
    }

    private fun isValidUnquotedKey(str: String): Boolean {
        if (str.isEmpty()) return false
        val first = str[0]
        if (!first.isAlpha() && first != '_') return false
        for (i in 1 until str.length) {
            val c = str[i]
            if (!c.isAlpha() && !c.isDigit() && c != '_' && c != '.') return false
        }
        return true
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
