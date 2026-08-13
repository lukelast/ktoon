package com.lukelast.ktoon

/** Upper bound for [KtoonConfiguration.indentSize]; deeper is almost certainly a mistake. */
private const val MAX_INDENT_SIZE = 16

/**
 * Configuration for TOON format encoding and decoding.
 *
 * @property strictMode Enable strict validation of TOON format rules (default: true)
 * @property delimiter Delimiter character for array values and tabular format (default: COMMA)
 * @property indentSize Number of spaces per indentation level (default: 2)
 * @property sortFields Enable alphabetical sorting of object fields (default: false)
 * @property encodeDefaults Enable encoding of default property values (default: true)
 */
data class KtoonConfiguration(
    val strictMode: Boolean = true,
    val delimiter: Delimiter = Delimiter.COMMA,
    val indentSize: Int = 2,
    val sortFields: Boolean = false,
    val encodeDefaults: Boolean = true,
) {
    init {
        require(indentSize > 0) { "indentSize must be positive, got $indentSize" }
        require(indentSize <= MAX_INDENT_SIZE) {
            "indentSize must be <= $MAX_INDENT_SIZE, got $indentSize"
        }
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
        val Default = KtoonConfiguration()
    }
}

/** Builder class for constructing ToonConfiguration instances. */
class KtoonConfigurationBuilder {
    /** Delimiter character used to separate values in inline arrays and tabular format. */
    var delimiter: KtoonConfiguration.Delimiter = KtoonConfiguration.Delimiter.COMMA

    /** How many spaces to use for each indentation level. */
    var indentSize: Int = 2

    /** Used for decoding validation. */
    var strictMode: Boolean = true

    /**
     * Enable alphabetical sorting of object fields. Default is field order stays as they are
     * originally defined. Note this goes against the TOON specification.
     */
    var sortFields: Boolean = false

    /**
     * Controls whether properties whose values are equal to their declared default values are
     * written during serialization.
     *
     * When `true` (default), all properties are encoded even if they currently hold the same value
     * as their default. This is usually what you want because the LLM does not know what the
     * default values are.
     *
     * When `false`, properties whose values match their defaults are omitted from the output. This
     * reduces the size of the serialized data. This allows you to strip out fields you don't want.
     * Consider using the [kotlinx.serialization.EncodeDefault] annotation on specific properties
     * instead for more fine-grained control.
     */
    var encodeDefaults: Boolean = true

    fun build(): KtoonConfiguration =
        KtoonConfiguration(
            strictMode = strictMode,
            delimiter = delimiter,
            indentSize = indentSize,
            sortFields = sortFields,
            encodeDefaults = encodeDefaults,
        )
}
