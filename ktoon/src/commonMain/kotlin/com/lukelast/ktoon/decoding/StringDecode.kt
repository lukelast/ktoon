package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonParsingException

/**
 * Unescapes a quoted token per §7.1, or returns the input unchanged when it is not quoted.
 *
 * A token beginning with `"` MUST end at its closing quote (§7.4, both modes): an unterminated
 * quote or content after the closing quote is an error. Valid escapes are `\\`, `\"`, `\n`, `\r`,
 * `\t`, and `\uXXXX`.
 */
internal fun unquote(str: String, line: Int = -1, column: Int = -1): String {
    if (!str.startsWith('"')) return str

    val sb = StringBuilder(str.length - 2)
    var i = 1
    while (i < str.length) {
        when (val c = str[i]) {
            '\\' -> {
                if (i + 1 >= str.length)
                    throw KtoonParsingException.invalidEscapeSequence("\\", line, column + i)
                when (val next = str[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 5 >= str.length)
                            throw KtoonParsingException.invalidEscapeSequence(
                                str.substring(i),
                                line,
                                column + i,
                            )
                        val hex = str.substring(i + 2, i + 6)
                        val code =
                            hex.toIntOrNull(16)
                                ?: throw KtoonParsingException.invalidEscapeSequence(
                                    "\\u$hex",
                                    line,
                                    column + i,
                                )
                        // §7.1: escapes must denote Unicode scalar values; surrogate code points
                        // are never valid (supplementary characters appear as literal UTF-8).
                        if (code in 0xD800..0xDFFF) {
                            throw KtoonParsingException.invalidEscapeSequence(
                                "\\u$hex",
                                line,
                                column + i,
                            )
                        }
                        sb.append(code.toChar())
                        i += 4
                    }
                    else ->
                        throw KtoonParsingException.invalidEscapeSequence(
                            "\\$next",
                            line,
                            column + i,
                        )
                }
                i += 2
            }
            '"' -> {
                // Closing quote: it must be the token's final character (§7.4).
                if (i != str.length - 1)
                    throw KtoonParsingException.unterminatedString(line, column)
                return sb.toString()
            }
            else -> {
                sb.append(c)
                i++
            }
        }
    }
    throw KtoonParsingException.unterminatedString(line, column)
}

/** Trims surrounding spaces from a token: exactly U+0020, no other whitespace (§12). */
internal fun String.trimSpaces(): String = trim(' ')

internal fun splitRespectingQuotes(content: String, delimiter: Char): List<String> {
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

/**
 * Finds the first unquoted occurrence of a character. Returns -1 if not found. Quoted sections are
 * delimited by unescaped double quotes.
 */
internal fun findUnquoted(str: String, target: Char, startIndex: Int = 0): Int {
    var inQuotes = false
    var escapeNext = false
    for (i in startIndex until str.length) {
        val char = str[i]
        when {
            escapeNext -> escapeNext = false
            char == '\\' -> escapeNext = true
            char == '"' -> inQuotes = !inQuotes
            char == target && !inQuotes -> return i
        }
    }
    return -1
}
