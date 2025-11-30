package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration

/** Buffered writer for TOON format output with indentation management. */
internal class ToonWriter(private val config: ToonConfiguration, initialCapacity: Int = 1024) {
    private val buffer = StringBuilder(initialCapacity)
    private var atLineStart = true

    private val indentCache = Array(17) { " ".repeat(it * config.indentSize) }

    fun writeIndent(level: Int) {
        if (!atLineStart) return
        buffer.append(if (level < indentCache.size) indentCache[level] else " ".repeat(level * config.indentSize))
        atLineStart = false
    }

    fun writeKey(key: String) { buffer.append(key).append(':') }
    fun writeKeyValue(key: String, value: String) { buffer.append(key).append(": ").append(value) }
    fun write(value: String) { buffer.append(value) }
    fun writeSpace() { buffer.append(' ') }
    fun writeDelimiter() { buffer.append(config.delimiter.char) }
    fun writeNewline() { buffer.append('\n'); atLineStart = true }
    fun writeDash() { buffer.append('-') }

    fun writeArrayHeader(key: String, length: Int, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') buffer.append(delimiter)
        buffer.append("]:") 
    }

    fun writeTabularArrayHeader(key: String, length: Int, fields: List<String>, delimiter: Char) {
        buffer.append(key).append('[').append(length)
        if (delimiter != ',') buffer.append(delimiter)
        buffer.append("]{")
            .append(fields.joinToString(delimiter.toString()))
            .append("}:")
    }

    override fun toString(): String = buffer.toString()
    fun clear() { buffer.clear(); atLineStart = true }
    fun length(): Int = buffer.length
}
