package com.lukelast.ktoon

/**
 * Configuration for TOON format encoding and decoding.
 *
 * @property strictMode Enable strict validation of TOON format rules (default: true)
 * @property keyFolding Enable collapsing nested single-key objects into dotted notation (default:
 *   false)
 * @property pathExpansion Enable expanding dotted keys into nested structures when decoding
 *   (default: false)
 * @property delimiter Delimiter character for array values and tabular format (default: COMMA)
 * @property indentSize Number of spaces per indentation level (default: 2)
 */
data class ToonConfiguration(
    val strictMode: Boolean = true,
    val keyFolding: Boolean = false,
    val pathExpansion: Boolean = false,
    val delimiter: Delimiter = Delimiter.COMMA,
    val indentSize: Int = 2,
) {
    init {
        require(indentSize > 0) { "indentSize must be positive, got $indentSize" }
        require(indentSize <= 16) { "indentSize must be <= 16, got $indentSize" }
    }

    /** Delimiter character for separating values in inline arrays and tabular format. */
    enum class Delimiter(val char: Char, val displayName: String) {
        /** Comma delimiter (default) - most common and readable */
        COMMA(',', "comma"),

        /** Tab delimiter - useful when values may contain commas */
        TAB('\t', "tab"),

        /** Pipe delimiter - alternative when both commas and tabs might appear in values */
        PIPE('|', "pipe");

        override fun toString(): String = displayName
    }

    companion object {
        /** Default configuration with strict mode enabled and standard formatting. */
        val Default = ToonConfiguration()

        /**
         * Compact configuration optimized for minimal output size. Enables key folding for more
         * compact representation.
         */
        val Compact = ToonConfiguration(keyFolding = true)
    }
}

/**
 * Builder function for creating ToonConfiguration with a DSL-style syntax.
 *
 * Example:
 * ```
 * val config = ToonConfiguration {
 *     strictMode = false
 *     keyFolding = true
 *     indentSize = 4
 * }
 * ```
 */
inline fun ToonConfiguration(
    builderAction: ToonConfigurationBuilder.() -> Unit
): ToonConfiguration {
    return ToonConfigurationBuilder().apply(builderAction).build()
}

/** Builder class for constructing ToonConfiguration instances. */
class ToonConfigurationBuilder {
    var strictMode: Boolean = true
    var keyFolding: Boolean = false
    var pathExpansion: Boolean = false
    var delimiter: ToonConfiguration.Delimiter = ToonConfiguration.Delimiter.COMMA
    var indentSize: Int = 2

    fun build(): ToonConfiguration =
        ToonConfiguration(
            strictMode = strictMode,
            keyFolding = keyFolding,
            pathExpansion = pathExpansion,
            delimiter = delimiter,
            indentSize = indentSize,
        )
}
