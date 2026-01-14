package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration

/**
 * Buffered writer for TOON format output. This is not thread safe.
 *
 * This implementation uses a CharArray-based approach similar to kotlinx.serialization's
 * JsonToStringWriter, avoiding StringBuilder overhead and leveraging optimized array operations.
 *
 * Benefits:
 * - Direct CharArray access skips compact string checks in StringBuilder
 * - Batch copying via toCharArray() is faster than byte-by-byte operations
 * - inlining was shown in increase performance a few percent while benchmarking.
 */
@Suppress("NOTHING_TO_INLINE")
internal class ToonWriter(private val config: KtoonConfiguration) {
    private var array: CharArray = CharArray(2048)
    private var size = 0
    private var atLineStart = true
    private val delimiterChar = config.delimiter.char

    inline fun writeIndent(level: Int) {
        if (!atLineStart) return
        writeSpaces(level * config.indentSize)
        atLineStart = false
    }

    private inline fun writeSpaces(count: Int) {
        if (count == 0) return
        ensureAdditionalCapacity(count)
        array.fill(' ', size, size + count)
        size += count
    }

    inline fun writeKey(key: String) {
        write(key)
        write(':')
    }

    inline fun writeKeyValue(key: String, value: String) {
        write(key)
        write(':')
        write(' ')
        write(value)
    }

    inline fun write(value: String) {
        val length = value.length
        if (length == 0) return
        ensureAdditionalCapacity(length)
        value.toCharArray(array, size, 0, length)
        size += length
    }

    inline fun write(value: Char) {
        ensureAdditionalCapacity(1)
        array[size++] = value
    }

    inline fun write(value: Int) {
        write(value.toString())
    }

    inline fun writeSpace() {
        write(' ')
    }

    inline fun writeDelimiter() {
        write(delimiterChar)
    }

    inline fun writeNewline() {
        write('\n')
        atLineStart = true
    }

    fun writeDash() {
        write('-')
    }

    fun writeArrayHeader(key: String, length: Int, delimiter: Char) {
        write(key)
        write('[')
        write(length)
        if (delimiter != ',') write(delimiter)
        write(']')
        write(':')
    }

    fun writeTabularArrayHeader(key: String, length: Int, fields: List<String>, delimiter: Char) {
        write(key)
        write('[')
        write(length)
        if (delimiter != ',') write(delimiter)
        write(']')
        write('{')
        write(fields.joinToString(delimiter.toString()))
        write('}')
        write(':')
    }

    override fun toString(): String = array.concatToString(0, size)

    private inline fun ensureAdditionalCapacity(expected: Int) {
        val newSize = size + expected
        if (array.size <= newSize) {
            array = array.copyOf(newSize.coerceAtLeast(size * 2))
        }
    }
}
