package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonParsingException

/** Utility for quoting and unquoting strings according to TOON format rules. */
internal object StringQuoting {

    private val KEYWORDS = setOf("true", "false", "null")
    private val NUMBER_PATTERN = Regex("""^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?$""")
    private val WHITESPACE_OR_CONTROL = Regex("""^\s.*|.*\s$|[\u0000-\u001F]""")
    private val UNQUOTED_KEY_PATTERN = Regex("""^[A-Za-z_][A-Za-z0-9_.]*$""")
    private val SPECIAL_CHARS = setOf('"', '\\', '\n', '\r', '\t', ':', '[', ']', '{', '}')

    enum class QuotingContext { OBJECT_KEY, OBJECT_VALUE, ARRAY_ELEMENT, ROOT }

    fun needsQuoting(str: String, context: QuotingContext = QuotingContext.OBJECT_VALUE, delimiter: Char = ','): Boolean {
        if (str.isEmpty() || str in KEYWORDS || NUMBER_PATTERN.matches(str)) return true
        if (WHITESPACE_OR_CONTROL.containsMatchIn(str)) return true
        if (str.any { it in SPECIAL_CHARS }) return true
        if (str.startsWith("-")) return true
        return when (context) {
            QuotingContext.OBJECT_KEY -> !UNQUOTED_KEY_PATTERN.matches(str)
            QuotingContext.ARRAY_ELEMENT, QuotingContext.OBJECT_VALUE -> str.contains(delimiter)
            else -> false
        }
    }

    fun quote(str: String, context: QuotingContext = QuotingContext.OBJECT_VALUE, delimiter: Char = ','): String {
        if (!needsQuoting(str, context, delimiter)) return str
        return buildString(str.length + 2) {
            append('"')
            str.forEach {
                when (it) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(it)
                }
            }
            append('"')
        }
    }

    fun unquote(str: String, line: Int = -1, column: Int = -1): String {
        if (!str.startsWith('"')) return str
        if (!str.endsWith('"') || str.length < 2) throw ToonParsingException.unterminatedString(line, column)
        val content = str.substring(1, str.length - 1)
        if (!content.contains('\\')) return content
        return buildString(content.length) {
            var i = 0
            while (i < content.length) {
                if (content[i] == '\\') {
                    if (i + 1 >= content.length) throw ToonParsingException.invalidEscapeSequence("\\", line, column + i)
                    when (val next = content[i + 1]) {
                        '\\' -> append('\\')
                        '"' -> append('"')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        else -> throw ToonParsingException.invalidEscapeSequence("\\$next", line, column + i)
                    }
                    i += 2
                } else {
                    append(content[i++])
                }
            }
        }
    }
}
