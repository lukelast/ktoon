package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException
import com.lukelast.ktoon.KtoonValidationException
import com.lukelast.ktoon.util.isIdentifierSegment
import com.lukelast.ktoon.validation.ValidationEngine

/**
 * Parser for TOON tokens that builds a logical value structure.
 *
 * Converts the flat token stream from ToonLexer into a nested structure of ToonValue objects that
 * can be consumed by the decoder.
 *
 * Handles:
 * - Object structures (nested key-value pairs)
 * - All three array formats (inline, tabular, expanded)
 * - Primitive values
 * - Validation in strict mode
 */
internal class ToonReader(private val tokens: List<Token>, private val config: KtoonConfiguration) {
    private var position = 0
    private val validator = ValidationEngine(config)

    /** Reads the root value from the token stream. */
    fun readRoot(): ToonValue {
        if (tokens.isEmpty()) {
            return ToonValue.Object(emptyMap())
        }

        // Skip leading blank lines
        skipBlankLines()

        if (position >= tokens.size) {
            return ToonValue.Object(emptyMap())
        }

        // Determine root type from first token
        val result =
            when (val first = peek()) {
                is Token.ArrayHeader -> {
                    // §5: Root array header has NO KEY; if there's a key, treat as object field
                    // with array value
                    if (first.key.isEmpty()) {
                        readArray()
                    } else {
                        readObject(baseIndent = 0)
                    }
                }
                is Token.Dash -> {
                    readExpandedArrayFromRoot()
                }
                is Token.Key -> {
                    readObject(baseIndent = 0)
                }
                is Token.Value -> {
                    // Root primitive
                    advance()
                    parsePrimitive(first.content, first.line)
                }
                else -> {
                    throw KtoonParsingException("Unexpected token type at root", 1)
                }
            }

        // Check for trailing tokens in strict mode
        skipBlankLines() // Allow trailing blank lines
        if (config.strictMode && position < tokens.size) {
            val token = peek()
            throw KtoonParsingException("Unexpected content after root value", token.line)
        }

        return result
    }

    /** Reads an object (collection of key-value pairs). */
    private fun readObject(baseIndent: Int): ToonValue.Object {
        val properties = mutableMapOf<String, ToonValue>()

        while (position < tokens.size) {
            val token = peek()

            if (token is Token.BlankLine) {
                advance()
                continue
            }

            // Check if we've moved back to parent level
            val indent =
                when (token) {
                    is Token.Key -> token.indent
                    is Token.ArrayHeader -> token.indent
                    else -> -1 // Should not happen for valid properties
                }

            if (indent != -1) {
                if (indent < baseIndent) {
                    break
                }
                if (indent > baseIndent && config.strictMode) {
                    throw KtoonValidationException(
                        "Invalid indentation: expected $baseIndent, got $indent",
                        token.line,
                    )
                }
            }

            when (token) {
                is Token.Key -> {
                    advance()
                    val rawKey = token.name
                    val key = unquote(rawKey, token.line)
                    val value = readValue(token.indent)

                    insertProperty(properties, key, rawKey, value, token.line)
                }
                is Token.ArrayHeader -> {
                    // If ArrayHeader has no key, it's not a property of this object
                    if (token.key.isEmpty()) {
                        break
                    }
                    val arrayValue = readArray()
                    if (arrayValue is ToonValue.Array) {
                        val rawKey = token.key
                        val key = unquote(rawKey, token.line)
                        insertProperty(properties, key, rawKey, arrayValue, token.line)
                    }
                }
                else -> {
                    // Unexpected token
                    break
                }
            }
        }

        return ToonValue.Object(properties)
    }

    private fun insertProperty(
        properties: MutableMap<String, ToonValue>,
        key: String,
        rawKey: String,
        value: ToonValue,
        line: Int,
    ) {
        // Expand paths if enabled and key is not quoted
        if (config.pathExpansion && !rawKey.startsWith("\"") && key.contains('.')) {
            val parts = key.split('.')
            // Only expand if all parts are valid identifiers (Safe Mode)
            if (parts.all { it.isIdentifierSegment() }) {
                insertExpandedProperty(properties, parts, value, line)
                return
            }
        }

        // Regular insertion (or fallback if expansion conditions not met)
        if (config.strictMode && properties.containsKey(key)) {
            throw KtoonValidationException.duplicateKey(key, line)
        }
        properties[key] = value
    }

    private fun insertExpandedProperty(
        properties: MutableMap<String, ToonValue>,
        parts: List<String>,
        value: ToonValue,
        line: Int,
    ) {
        val part = parts[0]

        if (parts.size == 1) {
            // Leaf node
            if (config.strictMode && properties.containsKey(part)) {
                throw KtoonValidationException.duplicateKey(part, line)
            }
            properties[part] = value
            return
        }

        // Intermediate node
        val existing = properties[part]
        val nextProperties: MutableMap<String, ToonValue>

        if (existing == null) {
            nextProperties = mutableMapOf()
            properties[part] = ToonValue.Object(nextProperties)
        } else if (existing is ToonValue.Object) {
            // Copy existing properties to mutable map to allow merging
            nextProperties = existing.properties.toMutableMap()
            properties[part] = ToonValue.Object(nextProperties)
        } else {
            // Conflict: existing is primitive/array, but we need object
            if (config.strictMode) {
                throw KtoonValidationException(
                    "Path expansion conflict: '$part' is already defined as ${existing::class.simpleName}",
                    line,
                )
            }
            // Non-strict: Overwrite with new object (LWW)
            nextProperties = mutableMapOf()
            properties[part] = ToonValue.Object(nextProperties)
        }

        insertExpandedProperty(nextProperties, parts.drop(1), value, line)
    }

    /** Reads a value (can be primitive, object, or array). */
    private fun readValue(parentIndent: Int): ToonValue {
        skipBlankLines()
        if (position >= tokens.size) {
            return ToonValue.Null
        }

        return when (val token = peek()) {
            is Token.Value -> {
                advance()
                parsePrimitive(token.content, token.line)
            }
            is Token.ArrayHeader -> {
                if (token.key.isNotEmpty()) {
                    readObject(baseIndent = parentIndent + config.indentSize)
                } else {
                    readArray()
                }
            }
            is Token.Key -> {
                // Nested object
                readObject(baseIndent = parentIndent + config.indentSize)
            }
            else -> {
                ToonValue.Null
            }
        }
    }

    /** Reads an array in any format (inline, tabular, or expanded). */
    private fun readArray(): ToonValue.Array {
        val header = consume<Token.ArrayHeader>()

        // Determine array format
        return when {
            // Tabular format (has fields)
            header.fields != null -> {
                readTabularArray(header)
            }
            // Check next token to distinguish inline vs expanded
            position < tokens.size && peekIgnoringBlankLines() is Token.InlineArrayValue -> {
                skipBlankLines() // Should not happen for inline but just in case
                readInlineArray(header)
            }
            // Expanded format (dash markers)
            else -> {
                readExpandedArray(header)
            }
        }
    }

    /** Reads an inline array: `key[3]: val1,val2,val3` */
    private fun readInlineArray(header: Token.ArrayHeader): ToonValue.Array {
        val valueToken = consume<Token.InlineArrayValue>()

        // Split by delimiter (§12: surrounding whitespace SHOULD be tolerated; empty tokens decode
        // to empty string)
        val values =
            splitRespectingQuotes(valueToken.content, header.delimiter.char)
                .map { it.trim() }
                // Note: Empty tokens (after trimming) decode to empty string per §12
                .map { parsePrimitive(it, valueToken.line) }

        // Validate array length in strict mode
        validator.validateArrayLength(header.length, values.size, header.line)

        return ToonValue.Array(values)
    }

    /** Reads a tabular array: `key[2]{id,name}:\n 1,Alice\n 2,Bob` */
    private fun readTabularArray(header: Token.ArrayHeader): ToonValue.Array {
        val fields =
            header.fields?.map { unquote(it.trim(), header.line) }
                ?: throw KtoonParsingException("Tabular array missing field list", header.line)

        val elements = mutableListOf<ToonValue.Object>()

        // Check if this array header is on the same line as a preceding dash (nested in expanded
        // array)
        // If so, and it has NO key (it's the direct value of the list item), the expected
        // indentation
        // for children should be relative to the dash.
        val previousToken = if (position >= 2) tokens[position - 2] else null
        val isOnDashLine = previousToken is Token.Dash && previousToken.line == header.line

        val baseIndent =
            if (isOnDashLine && header.key.isEmpty()) header.indent - config.indentSize
            else header.indent
        val expectedIndent = baseIndent + config.indentSize

        while (position < tokens.size) {
            val token = peek()

            if (token is Token.BlankLine) {
                validator.validateNoBlankLinesInArray(true, token.line)
                advance()
                continue
            }

            // Check if this is a row at the correct indentation
            if (token is Token.Key && token.indent == expectedIndent) {
                // This is actually a simple key-value, not a tabular row
                // Parse it as a row of values
                processTabularRow(token.name, header, fields, elements, token.line)
                advance()
            } else if (token is Token.Value) {
                // Value-only line (no key) - this is a tabular row
                processTabularRow(token.content, header, fields, elements, token.line)
                advance()
            } else {
                // End of array
                break
            }

            // Check if we have all expected elements
            if (elements.size >= header.length) {
                break
            }
        }

        // Validate array length
        validator.validateArrayLength(header.length, elements.size, header.line)

        return ToonValue.Array(elements)
    }

    private fun processTabularRow(
        content: String,
        header: Token.ArrayHeader,
        fields: List<String>,
        elements: MutableList<ToonValue.Object>,
        line: Int,
    ) {
        val values =
            splitRespectingQuotes(content, header.delimiter.char)
                .map { it.trim() }
                .map { parsePrimitive(it, line) }

        // Validate field count
        validator.validateTabularRow(fields.size, values.size, elements.size, line)

        // Create object from fields and values
        val obj = fields.zip(values).toMap()
        elements.add(ToonValue.Object(obj))
    }

    /** Reads an expanded array: `key[2]:\n - val1\n - val2` */
    private fun readExpandedArray(header: Token.ArrayHeader): ToonValue.Array {
        val elements = mutableListOf<ToonValue>()

        // Check if this array header is on the same line as a preceding dash (nested in expanded
        // array)
        // If so, and it has NO key (it's the direct value of the list item), the expected
        // indentation
        // for children should be relative to the dash.
        // If it HAS a key (it's a property of an object in the list), it introduces a new level,
        // so indentation should be relative to the header.
        val previousToken = if (position >= 2) tokens[position - 2] else null
        val isOnDashLine = previousToken is Token.Dash && previousToken.line == header.line

        val baseIndent =
            if (isOnDashLine && header.key.isEmpty()) header.indent - config.indentSize
            else header.indent
        val expectedIndent = baseIndent + config.indentSize

        while (position < tokens.size) {
            val token = peek()

            if (token is Token.BlankLine) {
                validator.validateNoBlankLinesInArray(true, token.line)
                advance()
                continue
            }

            if (token is Token.Dash && token.indent == expectedIndent) {
                advance()
                val value = readValue(expectedIndent)
                elements.add(value)
            } else if (token is Token.Key && token.indent < expectedIndent) {
                // Back to parent level
                break
            } else {
                break
            }

            // Check if we have all expected elements
            if (elements.size >= header.length) {
                break
            }
        }

        // Validate array length
        validator.validateArrayLength(header.length, elements.size, header.line)

        return ToonValue.Array(elements)
    }

    /** Reads an expanded array from root (no header, starts with dashes). */
    private fun readExpandedArrayFromRoot(): ToonValue.Array {
        val elements = mutableListOf<ToonValue>()

        while (position < tokens.size) {
            val token = peek()

            if (token is Token.BlankLine) {
                // Root array allows blank lines? Spec says "Blank lines are not allowed within
                // arrays in strict mode"
                // If it's a root array, it's still an array.
                validator.validateNoBlankLinesInArray(true, token.line)
                advance()
                continue
            }

            if (token is Token.Dash) {
                advance()
                val value = readValue(0)
                elements.add(value)
            } else {
                break
            }
        }

        return ToonValue.Array(elements)
    }

    /** Parses a primitive value from a string. */
    private fun parsePrimitive(content: String, line: Int): ToonValue {
        // Check if value is quoted (§7.4 - quoting disambiguates type)
        val isQuoted = content.startsWith('"')

        // Unquote if quoted
        val unquoted = unquote(content, line)

        // If originally quoted, return as string (prevents parsing "42" as number, etc.)
        if (isQuoted) {
            return ToonValue.String(unquoted)
        }

        // Check for null
        if (unquoted == "null") {
            return ToonValue.Null
        }

        // Check for boolean
        if (unquoted == "true") {
            return ToonValue.Boolean(true)
        }
        if (unquoted == "false") {
            return ToonValue.Boolean(false)
        }

        // Check for forbidden leading zeros (§4 - must be treated as string)
        if (hasLeadingZero(unquoted)) {
            return ToonValue.String(unquoted)
        }

        // Try to parse as number
        val numberValue = tryParseNumber(unquoted)
        if (numberValue != null) {
            return numberValue
        }

        // Default to string
        return ToonValue.String(unquoted)
    }

    /** Checks if a string has a forbidden leading zero (§4). */
    private fun hasLeadingZero(str: String): Boolean {
        if (str.isEmpty() || str.length == 1) return false
        if (str[0] != '0') return false
        // "0.5" or "0e6" are valid, but "05" is not
        val secondChar = str[1]
        return secondChar in '0'..'9' // Leading zero followed by digit
    }

    /** Tries to parse a string as a number. Returns null if not a valid number. */
    private fun tryParseNumber(str: String): ToonValue? {
        // Try integer first (for simple cases without exponents)
        val intValue = str.toIntOrNull()
        if (intValue != null) {
            return ToonValue.Number(intValue)
        }

        // Try long
        val longValue = str.toLongOrNull()
        if (longValue != null) {
            return ToonValue.Number(longValue)
        }

        // Try double (handles scientific notation)
        val doubleValue = str.toDoubleOrNull()
        if (doubleValue != null) {
            // If the double has no fractional part and fits in Int/Long, store as integer
            if (doubleValue.isFinite() && doubleValue == Math.floor(doubleValue)) {
                // Check if it fits in Int
                if (doubleValue >= Int.MIN_VALUE && doubleValue <= Int.MAX_VALUE) {
                    return ToonValue.Number(doubleValue.toInt())
                }
                // Check if it fits in Long
                if (doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE) {
                    return ToonValue.Number(doubleValue.toLong())
                }
            }
            return ToonValue.Number(doubleValue)
        }

        return null
    }

    /** Peeks at the current token without consuming it. */
    private fun peek(): Token {
        if (position >= tokens.size) {
            throw KtoonParsingException.unexpectedEndOfInput("more tokens")
        }
        return tokens[position]
    }

    /** Advances to the next token and returns the current one. */
    private fun advance(): Token {
        val token = peek()
        position++
        return token
    }

    /** Consumes a token of the expected type. */
    private inline fun <reified T : Token> consume(): T {
        val token = peek()
        if (token !is T) {
            throw KtoonParsingException(
                "Expected ${T::class.simpleName}, got ${token::class.simpleName}",
                -1,
            )
        }
        position++
        return token
    }

    /** Skips blank lines in the token stream. */
    private fun skipBlankLines() {
        while (position < tokens.size && tokens[position] is Token.BlankLine) {
            position++
        }
    }

    /** Peeks at the next token, skipping blank lines. */
    private fun peekIgnoringBlankLines(): Token {
        var p = position
        while (p < tokens.size && tokens[p] is Token.BlankLine) {
            p++
        }
        if (p >= tokens.size) {
            throw KtoonParsingException.unexpectedEndOfInput("more tokens")
        }
        return tokens[p]
    }
}

/** Represents a parsed TOON value. */
internal sealed class ToonValue {
    /** Null value */
    object Null : ToonValue()

    /** Boolean value */
    data class Boolean(val value: kotlin.Boolean) : ToonValue()

    /** Numeric value (Int, Long, or Double) */
    data class Number(val value: kotlin.Number) : ToonValue()

    /** String value */
    data class String(val value: kotlin.String) : ToonValue()

    /** Object (map of key-value pairs) */
    data class Object(val properties: Map<kotlin.String, ToonValue>) : ToonValue()

    /** Array (list of values) */
    data class Array(val elements: List<ToonValue>) : ToonValue()
}
