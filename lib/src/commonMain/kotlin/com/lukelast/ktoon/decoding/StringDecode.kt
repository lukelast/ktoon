package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonParsingException

internal fun unquote(str: String, line: Int = -1, column: Int = -1): String {
    if (!str.startsWith('"')) return str
    if (!str.endsWith('"') || str.length < 2)
        throw KtoonParsingException.Companion.unterminatedString(line, column)
    val content = str.substring(1, str.length - 1)
    if (content.indexOf('\\') == -1) return content

    val sb = StringBuilder(content.length)
    var i = 0
    val len = content.length
    while (i < len) {
        val c = content[i]
        if (c == '\\') {
            if (i + 1 >= len)
                throw KtoonParsingException.Companion.invalidEscapeSequence("\\", line, column + i)
            when (val next = content[i + 1]) {
                '\\' -> sb.append('\\')
                '"' -> sb.append('"')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                else ->
                    throw KtoonParsingException.Companion.invalidEscapeSequence(
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
