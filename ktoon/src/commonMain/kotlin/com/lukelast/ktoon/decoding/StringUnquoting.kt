package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonParsingException
import com.lukelast.ktoon.util.indexOfUnpairedSurrogate
import com.lukelast.ktoon.util.toCodePointLabel

/** Number of hex digits in a `\uXXXX` escape (§7.1). */
private const val UNICODE_ESCAPE_DIGITS = 4

private const val HEX_RADIX = 16

/** Surrogate code points, which a `\uXXXX` escape may never denote (§7.1). */
private const val MIN_SURROGATE = 0xD800
private const val MAX_SURROGATE = 0xDFFF

/** First character allowed to appear literally; C0 controls below it must be escaped (§7.1). */
private const val FIRST_LITERAL_CHAR = 0x20

/**
 * Unescapes a quoted token per §7.1, or returns the input unchanged when it is not quoted.
 *
 * A token beginning with `"` MUST end at its closing quote (§7.4, both modes): an unterminated
 * quote or content after the closing quote is an error. Valid escapes are `\\`, `\"`, `\n`, `\r`,
 * `\t`, and `\uXXXX`.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "ThrowsCount")
internal fun unquote(str: String, line: Int = -1, column: Int = -1): String {
    // §7.1: `unescaped-char` excludes U+D800–U+DFFF, so a literal lone surrogate is never valid
    // TOON text — decoding one would hand back a string the encoder cannot write again.
    val unpaired = str.indexOfUnpairedSurrogate()
    if (unpaired != -1) {
        throw KtoonParsingException(
            "Unpaired surrogate ${str[unpaired].toCodePointLabel()} in input",
            line,
            column + unpaired,
        )
    }
    if (!str.startsWith('"')) return str
    // A lone `"` opens a token that never closes; diagnose it before sizing the builder, whose
    // capacity would otherwise be negative.
    if (str.length == 1) throw KtoonParsingException.unterminatedString(line, column)

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
                        if (i + 1 + UNICODE_ESCAPE_DIGITS >= str.length)
                            throw KtoonParsingException.invalidEscapeSequence(
                                str.substring(i),
                                line,
                                column + i,
                            )
                        val hex = str.substring(i + 2, i + 2 + UNICODE_ESCAPE_DIGITS)
                        // §7.1: exactly 4HEXDIG. toIntOrNull(16) would also accept a leading
                        // sign, so validate the characters directly.
                        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                            throw KtoonParsingException.invalidEscapeSequence(
                                "\\u$hex",
                                line,
                                column + i,
                            )
                        }
                        val code = hex.toInt(HEX_RADIX)
                        // §7.1: escapes must denote Unicode scalar values; surrogate code points
                        // are never valid (supplementary characters appear as literal UTF-8).
                        if (code in MIN_SURROGATE..MAX_SURROGATE) {
                            throw KtoonParsingException.invalidEscapeSequence(
                                "\\u$hex",
                                line,
                                column + i,
                            )
                        }
                        sb.append(code.toChar())
                        i += UNICODE_ESCAPE_DIGITS
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
                // §7.1 quoted-char excludes literal C0 controls other than HTAB. Those values
                // must use one of the short escapes or a \uXXXX escape.
                if (c.code < FIRST_LITERAL_CHAR && c != '\t') {
                    val codePoint =
                        c.code.toString(HEX_RADIX).uppercase().padStart(UNICODE_ESCAPE_DIGITS, '0')
                    throw KtoonParsingException(
                        "Unescaped control character U+$codePoint",
                        line,
                        column + i,
                    )
                }
                sb.append(c)
                i++
            }
        }
    }
    throw KtoonParsingException.unterminatedString(line, column)
}

/** Trims surrounding spaces from a token: exactly U+0020, no other whitespace (§12). */
internal fun String.trimSpaces(): String = trim(' ')

/**
 * §7.4: a token is quoted only when its *first* character is `"`; a quote further inside an
 * unquoted token is ordinary data (`a"b: 1` is the key `a"b`). A quote therefore opens a quoted run
 * only at the start of the scanned text or directly after a character that begins a new token.
 */
internal fun opensQuotedToken(str: String, index: Int): Boolean {
    var i = index - 1
    while (i >= 0 && str[i] == ' ') i--
    return i < 0 || str[i] in TOKEN_START_CHARS
}

/** Characters after which a new token begins: delimiters, colons, and the bracket openers. */
private const val TOKEN_START_CHARS = ",|\t:[{"

internal fun splitRespectingQuotes(content: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    var escapeNext = false

    for (char in content) {
        if (escapeNext) {
            current.append(char)
            escapeNext = false
        } else if (char == '\\' && inQuotes) {
            current.append(char)
            escapeNext = true
        } else if (char == '"' && (inQuotes || current.isBlank())) {
            // Only a quote at the start of this cell opens a quoted token (§7.4).
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
 * Finds the first unquoted occurrence of a character. Returns -1 if not found. A quoted section
 * starts at a token-initial double quote and ends at the next unescaped one.
 */
internal fun findUnquoted(str: String, target: Char, startIndex: Int = 0): Int {
    var inQuotes = false
    var escapeNext = false
    for (i in startIndex until str.length) {
        val char = str[i]
        when {
            escapeNext -> escapeNext = false
            char == '\\' && inQuotes -> escapeNext = true
            char == '"' && inQuotes -> inQuotes = false
            char == '"' -> inQuotes = opensQuotedToken(str, i)
            char == target && !inQuotes -> return i
        }
    }
    return -1
}
