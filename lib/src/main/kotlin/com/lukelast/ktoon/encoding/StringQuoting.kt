package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException

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

    private fun Char.isAlpha(): Boolean {
        // 1. (c.code or 0x20): Force the char to lowercase (e.g., 'A' becomes 'a')
        // 2. Subtract 'a': Align the range to start at 0
        // 3. Check if result is < 26 (the number of letters in alphabet)
        return ((code or 0x20) - 'a'.code).toUInt() < 26u
    }

    private fun Char.isDigit(): Boolean {
        // Subtracts '0'. If c was less than '0', it wraps around to a huge
        // positive number (because of UInt). If it's 0-9, it stays small.
        return (this - '0').toUInt() < 10u
    }

    /**
     * Checks if a string is a valid IdentifierSegment per §1.9:
     * matching `^[A-Za-z_][A-Za-z0-9_]*$`
     */
    fun isIdentifierSegment(str: String): Boolean {
        if (str.isEmpty()) return false
        val first = str[0]
        if (!first.isAlpha() && first != '_') return false
        for (i in 1 until str.length) {
            val c = str[i]
            if (!c.isAlpha() && !c.isDigit() && c != '_') return false
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

    fun unquote(str: String, line: Int = -1, column: Int = -1): String {
        if (!str.startsWith('"')) return str
        if (!str.endsWith('"') || str.length < 2)
            throw KtoonParsingException.unterminatedString(line, column)
        val content = str.substring(1, str.length - 1)
        if (content.indexOf('\\') == -1) return content

        val sb = StringBuilder(content.length)
        var i = 0
        val len = content.length
        while (i < len) {
            val c = content[i]
            if (c == '\\') {
                if (i + 1 >= len)
                    throw KtoonParsingException.invalidEscapeSequence("\\", line, column + i)
                when (val next = content[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else ->
                        throw KtoonParsingException.invalidEscapeSequence(
                            "\\$next",
                            line,
                            column + i,
                        )
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    fun splitRespectingQuotes(content: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var escapeNext = false

        for (char in content) {
            if (escapeNext) {
                current.append(char)
                escapeNext = false
            } else if (char == '\\') {
                current.append(char)
                escapeNext = true
            } else if (char == '"') {
                inQuotes = !inQuotes
                current.append(char)
            } else if (char == delimiter && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
