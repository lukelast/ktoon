package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration

/**
 * Buffered writer for TOON format output with indentation management.
 *
 * Optimized for performance with pre-allocated buffer and efficient indentation handling.
 *
 * @property config Configuration for formatting (indentation size, delimiters, etc.)
 * @property initialCapacity Initial buffer capacity in characters (default: 1024)
 */
internal class ToonWriter(private val config: ToonConfiguration, initialCapacity: Int = 1024) {
    private val buffer = StringBuilder(initialCapacity)
    private var atLineStart = true
    private var currentIndent = 0

    /**
     * Pre-computed indentation strings for common levels (0-16). Avoids repeated string allocation
     * for indentation.
     */
    private val indentCache: Array<String> =
        Array(17) { level -> " ".repeat(level * config.indentSize) }

    /** Writes indentation for the current level. Only writes if at the start of a line. */
    fun writeIndent(level: Int) {
        if (!atLineStart) return

        currentIndent = level
        if (level < indentCache.size) {
            buffer.append(indentCache[level])
        } else {
            // Fallback for very deep nesting
            repeat(level * config.indentSize) { buffer.append(' ') }
        }
        atLineStart = false
    }

    /**
     * Writes a key with colon separator. Assumes the key is already properly quoted if necessary.
     *
     * @param key The key to write (should be pre-quoted if needed)
     */
    fun writeKey(key: String) {
        buffer.append(key).append(':')
    }

    /**
     * Writes a key-value pair with proper spacing.
     *
     * @param key The key (should be pre-quoted if needed)
     * @param value The value (should be pre-formatted)
     */
    fun writeKeyValue(key: String, value: String) {
        buffer.append(key).append(": ").append(value)
    }

    /** Writes a raw string value directly to the buffer. No quoting or escaping is performed. */
    fun writeString(value: String) {
        buffer.append(value)
    }

    /** Writes a number value. The number should already be normalized. */
    fun writeNumber(value: String) {
        buffer.append(value)
    }

    /** Writes a literal value (true, false, null). */
    fun writeLiteral(value: String) {
        buffer.append(value)
    }

    /** Writes a space character. */
    fun writeSpace() {
        buffer.append(' ')
    }

    /** Writes a colon character. */
    fun writeColon() {
        buffer.append(':')
    }

    /** Writes the configured delimiter character. */
    fun writeDelimiter() {
        buffer.append(config.delimiter.char)
    }

    /** Writes a newline and marks that we're at the start of a new line. */
    fun writeNewline() {
        buffer.append('\n')
        atLineStart = true
    }

    /** Writes a dash (used for expanded array elements). */
    fun writeDash() {
        buffer.append('-')
    }

    /** Writes array header for inline format: `key[length]:` or `key[length|]:` */
    fun writeInlineArrayHeader(key: String, length: Int, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') {
            buffer.append(delimiter)
        }
        buffer.append(']').append(':')
    }

    /**
     * Writes array header for tabular format: `key[length]{field1,field2}:`
     *
     * @param key Array key
     * @param length Array length
     * @param fields Field names for tabular format
     * @param delimiter The delimiter to use
     */
    fun writeTabularArrayHeader(
        key: String,
        length: Int,
        fields: List<String>,
        delimiter: Char,
    ) {
        buffer
            .append(key)
            .append('[')
            .append(length)
        if (delimiter != ',') {
            buffer.append(delimiter)
        }
        buffer
            .append(']')
            .append('{')
            .append(fields.joinToString(delimiter.toString()))
            .append('}')
            .append(':')
    }

    /** Writes array header for expanded format: `key[length]:` */
    fun writeExpandedArrayHeader(key: String, length: Int, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') {
            buffer.append(delimiter)
        }
        buffer.append(']').append(':')
    }

    /** Checks if we're currently at the start of a line. */
    fun isAtLineStart(): Boolean = atLineStart

    /** Gets the current buffer content as a string. */
    override fun toString(): String = buffer.toString()

    /** Clears the buffer. */
    fun clear() {
        buffer.clear()
        atLineStart = true
        currentIndent = 0
    }

    /** Gets the current buffer length. */
    fun length(): Int = buffer.length

    /** Appends a character directly to the buffer. Marks that we're no longer at line start. */
    fun append(char: Char) {
        buffer.append(char)
        atLineStart = char == '\n'
    }

    /**
     * Appends a string directly to the buffer. Marks that we're no longer at line start unless the
     * string is empty.
     */
    fun append(str: String) {
        if (str.isEmpty()) return
        buffer.append(str)
        atLineStart = str.endsWith('\n')
    }
}
