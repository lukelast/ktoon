package com.lukelast.ktoon

/**
 * Configuration for TOON format encoding and decoding.
 *
 * @property strictMode Enable strict validation of TOON format rules (default: true)
 * @property keyFolding Enable collapsing nested single-key objects into dotted notation (default:
 *   OFF)
 * @property flattenDepth Maximum depth for key folding (default: null, meaning Infinity)
 * @property pathExpansion Enable expanding dotted keys into nested structures when decoding
 *   (default: false)
 * @property delimiter Delimiter character for array values and tabular format (default: COMMA)
 * @property indentSize Number of spaces per indentation level (default: 2)
 * @property sortFields Enable alphabetical sorting of object fields (default: false)
 * @property encodeDefaults Enable encoding of default property values (default: true)
 */
data class KtoonConfiguration(
    val strictMode: Boolean = true,
    val keyFolding: KeyFoldingMode = KeyFoldingMode.OFF,
    val flattenDepth: Int? = null,
    val pathExpansion: Boolean = false,
    val delimiter: Delimiter = Delimiter.COMMA,
    val indentSize: Int = 2,
    val sortFields: Boolean = false,
    val encodeDefaults: Boolean = true,
) {
    init {
        require(indentSize > 0) { "indentSize must be positive, got $indentSize" }
        require(indentSize <= 16) { "indentSize must be <= 16, got $indentSize" }
        if (flattenDepth != null) {
            require(flattenDepth >= 0) { "flattenDepth must be non-negative, got $flattenDepth" }
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

        /**
         * Compact configuration optimized for minimal output size. Enables key folding for more
         * compact representation.
         */
        val Compact = KtoonConfiguration(keyFolding = KeyFoldingMode.SAFE)
    }
}

/** Modes for key folding. */
enum class KeyFoldingMode {
    /** No folding is performed. */
    OFF,
    /** Fold eligible chains according to safe rules. */
    SAFE,
}

/** Builder class for constructing ToonConfiguration instances. */
class KtoonConfigurationBuilder {
    /** Delimiter character used to separate values in inline arrays and tabular format. */
    var delimiter: KtoonConfiguration.Delimiter = KtoonConfiguration.Delimiter.COMMA

    /** How many spaces to use for each indentation level. */
    var indentSize: Int = 2

    /**
     * Off by default. When on, nested objects with a single field will be flattened. For example
     * `a.b.c: value`
     */
    var keyFolding: KeyFoldingMode = KeyFoldingMode.OFF

    /** Enable [keyFolding] */
    fun keyFoldingSafe() {
        keyFolding = KeyFoldingMode.SAFE
        pathExpansion = true
    }

    /**
     * Only used when [keyFolding] is on. Default is Infinity. This will set a limit to how many
     * objects can be flattened.
     */
    var flattenDepth: Int? = null

    /** Used for decoding validation. */
    var strictMode: Boolean = true

    /** A decoder setting. */
    var pathExpansion: Boolean = false

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
            keyFolding = keyFolding,
            flattenDepth = flattenDepth,
            pathExpansion = pathExpansion,
            delimiter = delimiter,
            indentSize = indentSize,
            sortFields = sortFields,
            encodeDefaults = encodeDefaults,
        )
}
