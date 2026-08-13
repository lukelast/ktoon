package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException
import com.lukelast.ktoon.util.isAsciiDigit

/**
 * Lexer for tokenizing TOON format text.
 *
 * Performs the lexical pre-pass of §5.1/§12 (BOM removal, comment-line stripping, trailing-space
 * stripping) and then line-by-line tokenization, tracking:
 * - Indentation levels
 * - Array and keyed headers, including nested field groups
 * - Key-value pairs
 * - Dash markers for arrays in list form
 * - Line positions for error reporting
 *
 * Malformed header candidates (§6, §14.2) throw in strict mode and fall through to key-value
 * parsing in non-strict mode.
 */
internal class ToonLexer(private val input: String, private val config: KtoonConfiguration) {
    private var currentLine = 0
    private val tokens = mutableListOf<Token>()

    /** Tokenizes the entire input and returns the token list. */
    fun tokenize(): List<Token> {
        // §12: a single U+FEFF at the very start is a byte-order mark, not content.
        val text = input.removePrefix("﻿")

        // §12: lines are separated by LF; a CR at the end of a line belongs to the CRLF
        // terminator, but a CR anywhere else is content.
        for ((lineIndex, splitLine) in text.split('\n').withIndex()) {
            currentLine = lineIndex + 1
            val rawLine = splitLine.removeSuffix("\r")
            // §5.1: a comment line has zero or more leading spaces (only spaces) followed by '#';
            // comment lines are removed before every other rule and never affect scopes.
            if (rawLine.trimStart(' ').startsWith('#')) continue
            // §12: trailing spaces are not part of the line's content.
            tokenizeLine(rawLine.trimEnd(' '))
        }

        return tokens
    }

    /** Processes a single line of TOON input. */
    private fun tokenizeLine(line: String) {
        // Emit blank line token. Only indentation characters are blank here; other Unicode
        // whitespace such as NBSP remains token content (§12).
        if (line.all { it == ' ' || it == '\t' }) {
            tokens.add(Token.BlankLine(currentLine))
            return
        }

        // Count leading spaces for indentation
        val indent = countIndentation(line)
        // §12: only spaces and, in non-strict mode, leading indentation tabs are indentation.
        // Other Unicode whitespace (for example NBSP) is token content and must be preserved.
        val trimmed = line.trimStart(' ', '\t')

        // Check for dash (list-item marker). Trailing spaces are already stripped, so a hyphen
        // followed only by spaces has become the bare marker "-" (§12).
        if (indent > 0 && (trimmed.startsWith("- ") || trimmed == "-")) {
            tokens.add(Token.Dash(indent, currentLine))
            val value = if (trimmed.length > 1) trimmed.substring(2).trimSpaces() else ""
            if (value.isNotEmpty()) {
                // §10 depth model: content on the hyphen line sits one level below the marker —
                // the same column as the item's continuation lines, which is the hyphen column
                // plus 2 only when indentSize is 2.
                tokenizeLineContent(value, indent + config.indentSize)
            }
            return
        }

        tokenizeLineContent(trimmed, indent)
    }

    @Suppress("ReturnCount")
    private fun tokenizeLineContent(content: String, indent: Int) {
        // §4/§9.1: the literal token [] (root, object-field, and list-item positions) is a value,
        // never a header candidate.
        if (content == "[]") {
            tokens.add(Token.Value(content, indent, currentLine))
            return
        }

        val colonIndex = findUnquoted(content, ':')
        val bracketStart = findUnquoted(content, '[')

        // §5.2: a line whose first unquoted colon precedes its first unquoted '[' is never a
        // header, and a line without any unquoted colon cannot be one either (a header requires
        // its terminating colon), so e.g. the row line `1,[]` is not a header candidate. Note the
        // keyed marker `[N:]` puts a colon inside the bracket segment, so header detection must
        // run on the full line, not on a split-at-first-colon key part.
        if (bracketStart != -1 && colonIndex != -1 && bracketStart < colonIndex) {
            when (val header = parseHeader(content, bracketStart)) {
                is HeaderParse.Match -> {
                    tokens.add(
                        Token.Header(
                            key = header.key,
                            length = header.length,
                            fields = header.fields,
                            delimiter = header.delimiter,
                            keyed = header.keyed,
                            rawContent = content,
                            indent = indent,
                            line = currentLine,
                        )
                    )
                    val valuePart = content.substring(header.colonIndex + 1).trimSpaces()
                    if (valuePart.isNotEmpty()) {
                        tokens.add(Token.InlineArrayValue(valuePart, currentLine))
                    }
                    return
                }
                is HeaderParse.Malformed -> {
                    // §6/§14.2: strict mode errors; non-strict mode falls through to key-value.
                    if (config.strictMode) {
                        throw KtoonParsingException.invalidArrayFormat(header.reason, currentLine)
                    }
                }
                is HeaderParse.NotAHeader -> {
                    // Not header-shaped at all (e.g. no closing bracket): key-value in both modes.
                }
            }
        }

        if (colonIndex == -1) {
            // No colon - scalar or row line; the reader decides by context (§5.2)
            tokens.add(Token.Value(content, indent, currentLine))
            return
        }

        // Regular key-value pair, split at the first unquoted colon (§7.4)
        val keyRaw = content.substring(0, colonIndex)
        val valuePart = content.substring(colonIndex + 1).trimSpaces()
        tokens.add(Token.Key(keyRaw.trimSpaces(), content, indent, currentLine))
        if (valuePart.isNotEmpty()) {
            tokens.add(Token.Value(valuePart, indent, currentLine))
        }
    }

    /**
     * Counts leading indentation. Tabs are not allowed in strict mode (§12); in non-strict mode a
     * leading tab is accepted as indentation and counts as one level (documented choice).
     */
    private fun countIndentation(line: String): Int {
        var count = 0
        for (char in line) {
            if (char == ' ') {
                count++
            } else if (char == '\t') {
                if (config.strictMode) {
                    throw KtoonParsingException(
                        "Tabs are not allowed in indentation (strict mode)",
                        currentLine,
                        count,
                    )
                }
                count += config.indentSize
            } else {
                break
            }
        }
        return count
    }

    private sealed interface HeaderParse {
        class Match(
            val key: String,
            val length: Int,
            val keyed: Boolean,
            val delimiter: KtoonConfiguration.Delimiter,
            val fields: List<FieldNode>?,
            /** Index of the header's terminating colon within the line content. */
            val colonIndex: Int,
        ) : HeaderParse

        class Malformed(val reason: String) : HeaderParse

        object NotAHeader : HeaderParse
    }

    /**
     * Parses a header candidate per §6 from the full line [content].
     *
     * Grammar: `key? '[' N ':'? delim? ']' fields-seg? ':'` where the colon inside the bracket
     * marks a keyed header and fields-seg is a brace-enclosed field list allowing nested field
     * groups. No whitespace or other content may appear between the key, bracket segment, field
     * list, and terminating colon.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
    private fun parseHeader(content: String, bracketStart: Int): HeaderParse {
        val bracketEnd = findUnquoted(content, ']', bracketStart)
        if (bracketEnd == -1) return HeaderParse.NotAHeader

        // §6 (v4.1): whitespace between a key and its bracket segment is a header syntax error.
        if (
            bracketStart > 0 &&
                (content[bracketStart - 1] == ' ' || content[bracketStart - 1] == '\t')
        ) {
            return HeaderParse.Malformed("whitespace between key and bracket segment")
        }

        val key = content.substring(0, bracketStart)
        val bracket = content.substring(bracketStart + 1, bracketEnd)

        // Parse the bracket segment: N, optional ':' (keyed), optional delimiter symbol.
        var i = 0
        while (i < bracket.length && bracket[i].isAsciiDigit()) i++
        if (i == 0) {
            return HeaderParse.Malformed(
                if (bracket.isEmpty()) "bracket segment without a length"
                else "invalid bracket length"
            )
        }
        val lengthStr = bracket.substring(0, i)
        if (lengthStr.length > 1 && lengthStr[0] == '0') {
            return HeaderParse.Malformed("leading zeros in bracket length")
        }
        val length =
            lengthStr.toIntOrNull() ?: return HeaderParse.Malformed("bracket length out of range")

        var keyed = false
        if (i < bracket.length && bracket[i] == ':') {
            keyed = true
            i++
        }
        val delimiter =
            when (val rest = bracket.substring(i)) {
                "" -> KtoonConfiguration.Delimiter.COMMA // absent always means comma (§6)
                "\t" -> KtoonConfiguration.Delimiter.TAB
                "|" -> KtoonConfiguration.Delimiter.PIPE
                else -> return HeaderParse.Malformed("invalid bracket segment content '$rest'")
            }

        // After the bracket segment: the terminating colon, or a field list starting immediately
        // with '{' and followed immediately by the terminating colon.
        var fields: List<FieldNode>? = null
        var pos = bracketEnd + 1
        if (pos < content.length && content[pos] == '{') {
            val braceEnd = findMatchingBrace(content, pos)
            if (braceEnd == -1) {
                return HeaderParse.Malformed("unterminated field list")
            }
            val parsed =
                parseFieldList(content.substring(pos + 1, braceEnd), delimiter.char)
                    ?: return HeaderParse.Malformed("invalid field list")
            fields = parsed
            pos = braceEnd + 1
        }

        if (pos >= content.length) {
            // A line with a bracket segment but no colon is not a header; it classifies as a
            // scalar or row line by context (§5.2).
            return HeaderParse.NotAHeader
        }
        if (content[pos] != ':') {
            return HeaderParse.Malformed(
                if (fields == null) "unexpected content after bracket segment"
                else "unexpected content after field list"
            )
        }

        // §6: a keyed header requires a field list.
        if (keyed && fields == null) {
            return HeaderParse.Malformed("keyed header without a field list")
        }

        // §6: a fields-bearing header — keyed or not — carries no inline content; decoding the
        // values as an inline array would silently drop the fields.
        if (fields != null && content.substring(pos + 1).trimSpaces().isNotEmpty()) {
            return HeaderParse.Malformed("content after a fields-bearing header's colon")
        }

        return HeaderParse.Match(key, length, keyed, delimiter, fields, pos)
    }

    /** Finds the unquoted '}' matching the unquoted '{' at [openIndex], or -1. */
    private fun findMatchingBrace(str: String, openIndex: Int): Int {
        var depth = 0
        var inQuotes = false
        var escapeNext = false
        for (i in openIndex until str.length) {
            val c = str[i]
            when {
                escapeNext -> escapeNext = false
                c == '\\' -> escapeNext = true
                c == '"' -> inQuotes = !inQuotes
                inQuotes -> {}
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    /**
     * Parses a field list (the content between braces) into field entries, allowing nested field
     * groups (§6, §9.3). Returns null if the list is malformed: empty, containing empty entries or
     * empty groups, or using a delimiter other than the active one.
     */
    @Suppress("ReturnCount")
    private fun parseFieldList(content: String, delimiter: Char): List<FieldNode>? {
        if (content.trimSpaces().isEmpty()) return null

        val entries = splitFieldEntries(content, delimiter) ?: return null
        val nodes = mutableListOf<FieldNode>()
        for (entryRaw in entries) {
            val entry = entryRaw.trimSpaces()
            if (entry.isEmpty()) return null
            val braceStart = findUnquoted(entry, '{')
            if (braceStart == -1) {
                if (containsOtherDelimiter(entry, delimiter)) return null
                nodes.add(FieldNode(entry, null))
            } else {
                val name = entry.substring(0, braceStart)
                if (name.isEmpty() || containsOtherDelimiter(name, delimiter)) return null
                val braceEnd = findMatchingBrace(entry, braceStart)
                if (braceEnd != entry.length - 1) return null
                val group =
                    parseFieldList(entry.substring(braceStart + 1, braceEnd), delimiter)
                        ?: return null
                nodes.add(FieldNode(name, group))
            }
        }
        return nodes
    }

    /**
     * Splits field-list content on the active delimiter, respecting quotes and brace nesting.
     * Returns null if quotes and braces are not balanced.
     */
    @Suppress("ReturnCount")
    private fun splitFieldEntries(content: String, delimiter: Char): List<String>? {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0
        var inQuotes = false
        var escapeNext = false
        for (c in content) {
            when {
                escapeNext -> {
                    escapeNext = false
                    current.append(c)
                }
                c == '\\' -> {
                    escapeNext = true
                    current.append(c)
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                inQuotes -> current.append(c)
                c == '{' -> {
                    depth++
                    current.append(c)
                }
                c == '}' -> {
                    depth--
                    if (depth < 0) return null
                    current.append(c)
                }
                c == delimiter && depth == 0 -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }
        if (inQuotes || depth != 0) return null
        result.add(current.toString())
        return result
    }

    /**
     * §14.2: the delimiter declared in the bracket segment must also be the field-list delimiter.
     * An unquoted occurrence of a different delimiter symbol in a field name means the field list
     * was written with the wrong delimiter.
     */
    private fun containsOtherDelimiter(name: String, delimiter: Char): Boolean {
        for (d in charArrayOf(',', '|', '\t')) {
            if (d != delimiter && findUnquoted(name, d) != -1) return true
        }
        return false
    }
}

/**
 * One member of a header's field list: a field name (raw, possibly quoted) optionally carrying a
 * nested field group (§6, §9.3).
 */
internal data class FieldNode(val name: String, val group: List<FieldNode>?)

/** Total number of leaf fields under [nodes], via a depth-first walk. */
internal fun leafFieldCount(nodes: List<FieldNode>): Int {
    var count = 0
    for ((_, group) in nodes) {
        count += if (group == null) 1 else leafFieldCount(group)
    }
    return count
}

/** Token types produced by the lexer. */
internal sealed interface Token {
    val line: Int

    /**
     * Object key token.
     *
     * @property name The key name (may be quoted), trimmed of surrounding spaces
     * @property rawContent The full line content after indentation (for row re-classification)
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class Key(
        val name: String,
        val rawContent: String,
        val indent: Int,
        override val line: Int,
    ) : Token

    /**
     * Value token (primitive or string).
     *
     * @property content The raw value content
     * @property indent Indentation of the line this value appeared on
     * @property line Line number (1-based)
     */
    data class Value(val content: String, val indent: Int, override val line: Int) : Token

    /**
     * Array or keyed header token.
     *
     * @property key Header key name (empty for keyless headers)
     * @property length Declared array length or entry count
     * @property fields Field entries for tabular/keyed form (null when absent)
     * @property delimiter Active delimiter declared by this header
     * @property keyed True for keyed headers `[N:...]` (§9.5)
     * @property rawContent The full line content after indentation (for entry-row re-classification
     *   at entry depth, §9.5)
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class Header(
        val key: String,
        val length: Int,
        val fields: List<FieldNode>?,
        val delimiter: KtoonConfiguration.Delimiter,
        val keyed: Boolean,
        val rawContent: String,
        val indent: Int,
        override val line: Int,
    ) : Token

    /**
     * Inline array value (delimiter-separated values after array header).
     *
     * @property content The raw value content
     * @property line Line number (1-based)
     */
    data class InlineArrayValue(val content: String, override val line: Int) : Token

    /**
     * Dash marker for a list-form array element.
     *
     * @property indent Indentation level in spaces
     * @property line Line number (1-based)
     */
    data class Dash(val indent: Int, override val line: Int) : Token

    /**
     * Blank line token.
     *
     * @property line Line number (1-based)
     */
    data class BlankLine(override val line: Int) : Token
}
