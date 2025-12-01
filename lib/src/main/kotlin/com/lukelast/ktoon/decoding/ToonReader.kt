package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonParsingException
import com.lukelast.ktoon.KtoonValidationException
import com.lukelast.ktoon.encoding.StringQuoting
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

        // Determine root type from first token
        val result = when (val first = peek()) {
            is Token.ArrayHeader -> {
                // §5: Root array header has NO KEY; if there's a key, treat as object field with array value
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

            // Check if we've moved back to parent level
            if (token is Token.Key && token.indent < baseIndent) {
                break
            }

            when (token) {
                is Token.Key -> {
                    advance()
                    val key = StringQuoting.unquote(token.name, token.line)

                    // Check for duplicate keys in strict mode
                    if (config.strictMode && properties.containsKey(key)) {
                        throw KtoonValidationException.duplicateKey(key, token.line)
                    }

                    // Read the value
                    val value = readValue(token.indent)
                    properties[key] = value
                }
                is Token.ArrayHeader -> {
                    val arrayValue = readArray()
                    if (arrayValue is ToonValue.Array) {
                        val key = StringQuoting.unquote(token.key, token.line)
                        properties[key] = arrayValue
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

    /** Reads a value (can be primitive, object, or array). */
    private fun readValue(parentIndent: Int): ToonValue {
        if (position >= tokens.size) {
            // At end of input - return empty object for list item context
            // This handles bare dash at end of expanded array
            return ToonValue.Object(emptyMap())
        }

        return when (val token = peek()) {
            is Token.Value -> {
                advance()
                parsePrimitive(token.content, token.line)
            }
            is Token.ArrayHeader -> {
                readArray()
            }
            is Token.Key -> {
                // Check if this key belongs to a nested object at the expected indent
                if (token.indent >= parentIndent + config.indentSize) {
                    readObject(baseIndent = parentIndent + config.indentSize)
                } else {
                    // Key is at or before parent level - this dash had no content
                    // Per §10: bare dash means empty object
                    ToonValue.Object(emptyMap())
                }
            }
            is Token.Dash -> {
                // Another dash before any content - this was a bare dash
                // Per §10: empty object list item
                ToonValue.Object(emptyMap())
            }
            else -> {
                // InlineArrayValue or other - should not normally happen
                ToonValue.Object(emptyMap())
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
            position < tokens.size && peek() is Token.InlineArrayValue -> {
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

        // Split by delimiter respecting quotes (§B.3: only split on unquoted delimiters)
        val values =
            splitDelimitedValues(valueToken.content, header.delimiter.char)
                // Note: Empty tokens (after trimming) decode to empty string per §12
                .map { parsePrimitive(it, valueToken.line) }

        // Validate array length in strict mode
        validator.validateArrayLength(header.length, values.size, header.line)

        return ToonValue.Array(values)
    }

    /** Reads a tabular array: `key[2]{id,name}:\n 1,Alice\n 2,Bob` */
    private fun readTabularArray(header: Token.ArrayHeader): ToonValue.Array {
        val fields =
            header.fields
                ?: throw KtoonParsingException("Tabular array missing field list", header.line)

        val elements = mutableListOf<ToonValue.Object>()
        val expectedIndent = header.indent + config.indentSize

        while (position < tokens.size) {
            val token = peek()

            // Check if this is a row at the correct indentation
            if (token is Token.Key && token.indent == expectedIndent) {
                // This is actually a simple key-value, not a tabular row
                // Parse it as a row of values
                val rowContent = token.name
                val values =
                    splitDelimitedValues(rowContent, header.delimiter.char)
                        .map { parsePrimitive(it, token.line) }

                // Validate field count
                validator.validateTabularRow(fields.size, values.size, elements.size, token.line)

                // Create object from fields and values
                val obj = fields.zip(values).toMap()
                elements.add(ToonValue.Object(obj))

                advance()
            } else if (token is Token.Value) {
                // Value-only line (no key) - this is a tabular row
                val values =
                    splitDelimitedValues(token.content, header.delimiter.char)
                        .map { parsePrimitive(it, token.line) }

                // Validate field count
                validator.validateTabularRow(fields.size, values.size, elements.size, token.line)

                // Create object from fields and values
                val obj = fields.zip(values).toMap()
                elements.add(ToonValue.Object(obj))

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

    /** Reads an expanded array: `key[2]:\n - val1\n - val2` */
    private fun readExpandedArray(header: Token.ArrayHeader): ToonValue.Array {
        val elements = mutableListOf<ToonValue>()
        val expectedIndent = header.indent + config.indentSize

        while (position < tokens.size) {
            val token = peek()

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

        while (position < tokens.size && peek() is Token.Dash) {
            advance()
            val value = readValue(0)
            elements.add(value)
        }

        return ToonValue.Array(elements)
    }

    /** Parses a primitive value from a string. */
    private fun parsePrimitive(content: String, line: Int): ToonValue {
        // Check if value is quoted (§7.4 - quoting disambiguates type)
        val isQuoted = content.startsWith('"')

        // Unquote if quoted
        val unquoted = StringQuoting.unquote(content, line)

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

    /**
     * Splits a string by the given delimiter, respecting quoted segments.
     *
     * Per TOON spec Appendix B.3:
     * - Iterates characters left-to-right while maintaining a current token and an inQuotes flag.
     * - On a double quote, toggle inQuotes.
     * - While inQuotes, treat backslash + next char as a literal pair (string parser validates later).
     * - Only split on the active delimiter when not in quotes (unquoted occurrences).
     * - Trim surrounding spaces around each token. Empty tokens decode to empty string.
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
