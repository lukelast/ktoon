package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException

/**
 * Lexer for tokenizing TOON format text.
 *
 * Performs line-by-line tokenization, tracking:
 * - Indentation levels
 * - Array headers (inline, tabular, expanded)
 * - Key-value pairs
 * - Dash markers for expanded arrays
 * - Line and column positions for error reporting
 *
 * The lexer produces a stream of tokens that the parser (ToonReader) will consume to build the
 * logical structure.
 */
internal class ToonLexer(private val input: String, private val config: KtoonConfiguration) {
    private var currentLine = 0
    private val tokens = mutableListOf<Token>()

    /** Tokenizes the entire input and returns the token list. */
    fun tokenize(): List<Token> {
        val lines = input.lines()

        for ((lineIndex, line) in lines.withIndex()) {
            currentLine = lineIndex + 1
            processLine(line)
        }

        return tokens
    }

    /** Processes a single line of TOON input. */
    private fun processLine(line: String) {
        // Skip empty lines
        if (line.isBlank()) {
            return
        }

        // Count leading spaces for indentation
        val indent = countLeadingSpaces(line)
        val trimmed = line.trimStart()

        // Check for dash (expanded array element marker)
        if (trimmed.startsWith("- ")) {
            tokens.add(Token.Dash(indent, currentLine))
            val afterDash = trimmed.substring(2)
            if (afterDash.isNotEmpty()) {
                // Content after dash is at dash's level + indentSize logically
                // But we pass the dash's indent and let the reader handle it
                processLineContent(afterDash, indent + config.indentSize)
            }
            return
        }

        // Check for bare dash (empty object list item)
        if (trimmed == "-") {
            tokens.add(Token.Dash(indent, currentLine))
            return
        }

        // Process as regular content
        processLineContent(trimmed, indent)
    }

    /** Processes the content of a line (after stripping indentation and optional dash). */
    private fun processLineContent(content: String, indent: Int) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        // Check for array header or key-value pair
        val colonIndex = findUnquotedColon(trimmed)

        if (colonIndex == -1) {
            // No colon - this is a continuation value or error
            tokens.add(Token.Value(trimmed, currentLine))
            return
        }

        // Split at colon
        val keyPart = trimmed.substring(0, colonIndex).trim()
        val valuePart = trimmed.substring(colonIndex + 1).trim()

        // Check if this is an array header
        val arrayMatch = parseArrayHeader(keyPart)
        if (arrayMatch != null) {
            tokens.add(
                Token.ArrayHeader(
                    key = arrayMatch.key,
                    length = arrayMatch.length,
                    fields = arrayMatch.fields,
                    delimiter = arrayMatch.delimiter,
                    indent = indent,
                    line = currentLine,
                )
            )
            // If there's a value part (inline array), add it
            if (valuePart.isNotEmpty()) {
                tokens.add(Token.InlineArrayValue(valuePart, currentLine))
            }
        } else {
            // Regular key-value pair
            tokens.add(Token.Key(keyPart, indent, currentLine))
            if (valuePart.isNotEmpty()) {
                tokens.add(Token.Value(valuePart, currentLine))
            }
        }
    }

    /** Counts leading spaces in a line. Tabs are not allowed in TOON indentation (strict mode). */
    private fun countLeadingSpaces(line: String): Int {
        var count = 0
        for (char in line) {
            if (char == ' ') {
                count++
            } else if (char == '\t' && config.strictMode) {
                throw KtoonParsingException(
                    "Tabs are not allowed in indentation (strict mode)",
                    currentLine,
                    count,
                )
            } else {
                break
            }
        }
        return count
    }

    /** Finds the first unquoted colon in a string. Returns -1 if no unquoted colon is found. */
    private fun findUnquotedColon(str: String): Int {
        return findFirstUnquoted(str, ':')
    }

    /** Finds the first unquoted occurrence of a character. Returns -1 if not found. */
    private fun findFirstUnquoted(str: String, target: Char): Int {
        var inQuotes = false
        var escapeNext = false

        for ((index, char) in str.withIndex()) {
            when {
                escapeNext -> {
                    escapeNext = false
                }
                char == '\\' -> {
                    escapeNext = true
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == target && !inQuotes -> {
                    return index
                }
            }
        }

        return -1
    }

    /**
     * Parses an array header pattern.
     *
     * Formats:
     * - `key[length]` - Inline or expanded array
     * - `key[length]{field1,field2}` - Tabular array
     * - `key[length	]` - Tab delimiter
     * - `key[length|]` - Pipe delimiter
     */
    private fun parseArrayHeader(keyPart: String): ArrayHeaderMatch? {
        // Find the first unquoted '[' to handle quoted keys like "key[test]"[3]
        val bracketStart = findFirstUnquoted(keyPart, '[')
        if (bracketStart == -1) return null

        val bracketEnd = keyPart.indexOf(']', bracketStart)
        if (bracketEnd == -1) return null

        val key = keyPart.substring(0, bracketStart).trim()
        val bracketContent = keyPart.substring(bracketStart + 1, bracketEnd)

        // Check for delimiter marker at end of bracket
        val delimiter =
            when {
                bracketContent.endsWith('\t') -> {
                    KtoonConfiguration.Delimiter.TAB
                }
                bracketContent.endsWith('|') -> {
                    KtoonConfiguration.Delimiter.PIPE
                }
                else -> {
                    config.delimiter // Use configured default
                }
            }

        // Remove delimiter marker if present
        val lengthStr = bracketContent.trimEnd('\t', '|')

        // Parse length
        val length =
            try {
                lengthStr.toInt()
            } catch (e: NumberFormatException) {
                throw KtoonParsingException.invalidArrayFormat(
                    "Invalid array length: '$lengthStr'",
                    currentLine,
                )
            }

        // Check for tabular format fields
        val fields =
            if (bracketEnd + 1 < keyPart.length && keyPart[bracketEnd + 1] == '{') {
                val fieldsEnd = keyPart.indexOf('}', bracketEnd + 2)
                if (fieldsEnd == -1) {
                    throw KtoonParsingException.invalidArrayFormat(
                        "Unterminated field list in tabular array header",
                        currentLine,
                    )
                }
                val fieldsStr = keyPart.substring(bracketEnd + 2, fieldsEnd)
                splitDelimitedValues(fieldsStr, delimiter.char)
            } else {
                null
            }

        return ArrayHeaderMatch(key, length, fields, delimiter)
    }

    /**
     * Splits a string by the given delimiter, respecting quoted segments.
     *
     * Per TOON spec Appendix B.3:
     * - Iterates characters left-to-right while maintaining a current token and an inQuotes flag.
     * - On a double quote, toggle inQuotes.
     * - While inQuotes, treat backslash + next char as a literal pair.
     * - Only split on the active delimiter when not in quotes.
     * - Trim surrounding spaces around each token.
     */
    private fun splitDelimitedValues(content: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes && c == '\\' && i + 1 < content.length -> {
                    // Escape sequence inside quotes - include both chars literally
                    current.append(c)
                    current.append(content[i + 1])
                    i += 2
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                    i++
                }
                c == delimiter && !inQuotes -> {
                    // Split point - add trimmed token and reset
                    result.add(current.toString().trim())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }

        // Add final token
        result.add(current.toString().trim())

        return result
    }

    /** Result of parsing an array header. */
    private data class ArrayHeaderMatch(
        val key: String,
        val length: Int,
        val fields: List<String>?,
        val delimiter: KtoonConfiguration.Delimiter,
    )
}

/** Token types produced by the lexer. */
internal sealed class Token {
    abstract val line: Int

    /**
     * Object key token.
     *
     * @property name The key name (may be quoted)
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class Key(val name: String, val indent: Int, override val line: Int) : Token()

    /**
     * Value token (primitive or string).
     *
     * @property content The raw value content
     * @property line Line number (1-based)
     */
    data class Value(val content: String, override val line: Int) : Token()

    /**
     * Array header token.
     *
     * @property key Array key name
     * @property length Declared array length
     * @property fields Field names for tabular format (null for inline/expanded)
     * @property delimiter Delimiter for this array
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class ArrayHeader(
        val key: String,
        val length: Int,
        val fields: List<String>?,
        val delimiter: KtoonConfiguration.Delimiter,
        val indent: Int,
        override val line: Int,
    ) : Token()

    /**
     * Inline array value (comma-separated values after array header).
     *
     * @property content The raw value content
     * @property line Line number (1-based)
     */
    data class InlineArrayValue(val content: String, override val line: Int) : Token()

    /**
     * Dash marker for expanded array element.
     *
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class Dash(val indent: Int, override val line: Int) : Token()

    /**
     * Tabular array row (values separated by delimiter).
     *
     * @property values List of raw value strings
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class TabularRow(val values: List<String>, val indent: Int, override val line: Int) : Token()
}
