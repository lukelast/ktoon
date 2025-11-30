package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonParsingException

/**
 * Utility for quoting and unquoting strings according to TOON format rules.
 *
 * TOON requires quoting strings when they:
 * - Are empty
 * - Have leading or trailing whitespace
 * - Match keywords (true, false, null)
 * - Could be parsed as numbers
 * - Contain special characters (quotes, backslashes, control characters)
 * - Contain delimiters (when used in arrays)
 * - Contain colons (in any context)
 */
internal object StringQuoting {

    private val KEYWORDS = setOf("true", "false", "null")

    /**
     * Number pattern that matches valid TOON numbers. Includes integers and decimals, with optional
     * minus sign.
     */
    private val NUMBER_PATTERN = Regex("""^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?$""")

    /** Pattern for strings with leading/trailing whitespace or control characters. */
    private val WHITESPACE_OR_CONTROL = Regex("""^\s.*|.*\s$|[\u0000-\u001F]""")

    /** Pattern for unquoted keys (identifier-like). */
    private val UNQUOTED_KEY_PATTERN = Regex("""^[A-Za-z_][A-Za-z0-9_.]*$""")

    /** Context in which a string is being quoted. */
    enum class QuotingContext {
        /** String is used as an object key */
        OBJECT_KEY,

        /** String is used as an object value */
        OBJECT_VALUE,

        /** String is an element in an array */
        ARRAY_ELEMENT,

        /** String is at the root level */
        ROOT,
    }

    /**
     * Determines if a string needs quoting based on TOON format rules.
     *
     * @param str The string to check
     * @param context The context in which the string appears
     * @param delimiter The delimiter character (relevant for array elements)
     * @return true if the string needs to be quoted
     */
    fun needsQuoting(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = ',',
    ): Boolean {
        // Empty strings must be quoted
        if (str.isEmpty()) return true

        // Keywords must be quoted
        if (str in KEYWORDS) return true

        // Strings matching number patterns must be quoted
        if (NUMBER_PATTERN.matches(str)) return true

        // Strings with leading/trailing whitespace or control characters must be quoted
        if (WHITESPACE_OR_CONTROL.containsMatchIn(str)) return true

        // Strings containing quotes, backslashes, or newlines must be quoted
        if (str.any { it == '"' || it == '\\' || it == '\n' || it == '\r' || it == '\t' }) {
            return true
        }

        // Strings containing colons must be quoted (spec section 7.2)
        if (str.contains(':')) return true

        // Context-specific checks
        when (context) {
            QuotingContext.ARRAY_ELEMENT -> {
                // Array elements containing the delimiter must be quoted
                if (str.contains(delimiter)) return true
            }
            QuotingContext.OBJECT_KEY -> {
                // Keys must match identifier pattern to be unquoted
                if (!UNQUOTED_KEY_PATTERN.matches(str)) return true
            }
            QuotingContext.OBJECT_VALUE -> {
                // Object values containing the delimiter must be quoted
                if (str.contains(delimiter)) return true
            }
            else -> {}
        }

        // Strings starting with hyphen (could be confused with list marker or negative number)
        if (str.startsWith("-")) return true

        // Strings starting with bracket or containing special TOON characters
        if (str.any { it == '[' || it == ']' || it == '{' || it == '}' }) {
            return true
        }

        return false
    }

    /**
     * Quotes a string with double quotes and escapes special characters.
     *
     * Only five escape sequences are allowed in TOON:
     * - \\ for backslash
     * - \" for double quote
     * - \n for newline
     * - \r for carriage return
     * - \t for tab
     *
     * @param str The string to quote
     * @param context The context in which the string appears
     * @param delimiter The delimiter character (relevant for array elements)
     * @return The quoted string, or the original if quoting is not needed
     */
    fun quote(
        str: String,
        context: QuotingContext = QuotingContext.OBJECT_VALUE,
        delimiter: Char = ',',
    ): String {
        if (!needsQuoting(str, context, delimiter)) {
            return str
        }

        return buildString(str.length + 2 + str.count { it == '\\' || it == '"' }) {
            append('"')
            str.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
    }

    /**
     * Unquotes a string and unescapes special characters.
     *
     * @param str The string to unquote (may or may not be quoted)
     * @param line Line number for error reporting
     * @param column Column number for error reporting
     * @return The unquoted and unescaped string
     * @throws ToonParsingException if the string has invalid escape sequences or is unterminated
     */
    fun unquote(str: String, line: Int = -1, column: Int = -1): String {
        // If not quoted, return as-is
        if (!str.startsWith('"')) {
            return str
        }

        // Check for unterminated string
        if (!str.endsWith('"') || str.length < 2) {
            throw ToonParsingException.unterminatedString(line, column)
        }

        // Remove quotes and unescape
        val content = str.substring(1, str.length - 1)
        return unescape(content, line, column)
    }

    /**
     * Unescapes a string by converting escape sequences to their actual characters.
     *
     * @param str The string to unescape
     * @param line Line number for error reporting
     * @param column Column number for error reporting
     * @return The unescaped string
     * @throws ToonParsingException if the string contains invalid escape sequences
     */
    private fun unescape(str: String, line: Int, column: Int): String {
        if (!str.contains('\\')) {
            return str
        }

        return buildString(str.length) {
            var i = 0
            while (i < str.length) {
                val char = str[i]
                if (char == '\\') {
                    if (i + 1 >= str.length) {
                        throw ToonParsingException.invalidEscapeSequence("\\", line, column + i)
                    }
                    val nextChar = str[i + 1]
                    when (nextChar) {
                        '\\' -> append('\\')
                        '"' -> append('"')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        else ->
                            throw ToonParsingException.invalidEscapeSequence(
                                "\\$nextChar",
                                line,
                                column + i,
                            )
                    }
                    i += 2
                } else {
                    append(char)
                    i++
                }
            }
        }
    }

    /** Determines the appropriate quoting context based on the position. */
    fun determineContext(isKey: Boolean, isInArray: Boolean): QuotingContext {
        return when {
            isKey -> QuotingContext.OBJECT_KEY
            isInArray -> QuotingContext.ARRAY_ELEMENT
            else -> QuotingContext.OBJECT_VALUE
        }
    }
}
