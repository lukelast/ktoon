package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration

/** Buffered writer for TOON format output with indentation management. */
internal class ToonWriter(private val config: ToonConfiguration, initialCapacity: Int = 1024) {
    private val buffer = StringBuilder(initialCapacity)
    private var atLineStart = true

    fun writeIndent(level: Int) {
        if (!atLineStart) return
        val totalSpaces = level * config.indentSize
        if (totalSpaces < spacesCache.size) {
            buffer.append(spacesCache[totalSpaces])
        } else {
            buffer.append(" ".repeat(totalSpaces))
        }
        atLineStart = false
    }

    fun writeKey(key: String) {
        buffer.append(key).append(':')
    }

    fun writeKeyValue(key: String, value: String) {
        buffer.append(key).append(": ").append(value)
    }

    fun write(value: String) {
        buffer.append(value)
    }

    fun write(value: Char) {
        buffer.append(value)
    }

    fun write(value: Int) {
        buffer.append(value)
    }

    fun writeSpace() {
        buffer.append(' ')
    }

    fun writeDelimiter() {
        buffer.append(config.delimiter.char)
    }

    fun writeNewline() {
        buffer.append('\n')
        atLineStart = true
    }

    fun writeDash() {
        buffer.append('-')
    }

    fun writeArrayHeader(key: String, length: Int, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') buffer.append(delimiter)
        buffer.append("]:")
    }

    fun writeTabularArrayHeader(key: String, length: Int, fields: List<String>, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') buffer.append(delimiter)
        buffer.append("]{").append(fields.joinToString(delimiter.toString())).append("}:")
    }

    override fun toString(): String = buffer.toString()

    fun length(): Int = buffer.length

    companion object {
        private val spacesCache = Array(256) { " ".repeat(it) }
    }
}
