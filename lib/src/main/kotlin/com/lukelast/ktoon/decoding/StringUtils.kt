package com.lukelast.ktoon.decoding

/**
 * Utility for splitting delimited values while respecting quoted segments.
 */
internal object StringUtils {

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
    fun splitDelimitedValues(content: String, delimiter: Char): List<String> {
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
