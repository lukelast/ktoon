package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.ToonConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a TOON test fixture file containing multiple test cases.
 *
 * Fixture files follow the schema defined at `C:\code\toon-format-spec\tests\fixtures.schema.json`.
 *
 * @property version TOON specification version (e.g., "1.4", "1.5")
 * @property category Test category (encode or decode)
 * @property description Brief description of what this fixture tests
 * @property tests List of individual test cases
 */
@Serializable
data class ToonFixture(
    val version: String,
    val category: FixtureCategory,
    val description: String,
    val tests: List<FixtureTestCase>,
) {
    fun isEncode(): Boolean = category == FixtureCategory.encode

    fun isDecode(): Boolean = category == FixtureCategory.decode
}

/** Test category: encode (JSON → TOON) or decode (TOON → JSON). */
@Serializable
enum class FixtureCategory {
    /** Encode tests: convert JSON input to TOON output */
    encode,

    /** Decode tests: convert TOON input to JSON output */
    decode,
}

/**
 * Individual test case within a fixture file.
 *
 * @property name Descriptive test name explaining what is being validated
 * @property input Input value - JSON value for encode tests, TOON string for decode tests
 * @property expected Expected output - TOON string for encode tests, JSON value for decode tests
 * @property shouldError If true, test expects an error to be thrown (default: false)
 * @property options Encoding/decoding configuration options
 * @property specSection Reference to relevant specification section (e.g., "7.2", "§9.3")
 * @property note Optional explanatory note about special cases or edge case behavior
 * @property minSpecVersion Minimum specification version required for this test
 */
@Serializable
data class FixtureTestCase(
    val name: String,
    val input: JsonElement,
    val expected: JsonElement,
    val shouldError: Boolean = false,
    val options: FixtureOptions = FixtureOptions(),
    val specSection: String? = null,
    val note: String? = null,
    val minSpecVersion: String? = null,
) {}

/**
 * Configuration options for encoding/decoding tests.
 *
 * All fields have defaults matching the TOON specification's default behavior.
 *
 * @property delimiter Array delimiter character: "," (comma), "\t" (tab), or "|" (pipe)
 * @property indent Number of spaces per indentation level (default: 2)
 * @property strict Enable strict validation (default: true)
 * @property keyFolding Key folding mode: "off" (default) or "safe"
 * @property flattenDepth Maximum depth to fold key chains when keyFolding is "safe"
 * @property expandPaths Path expansion mode: "off" (default) or "safe"
 */
@Serializable
data class FixtureOptions(
    val delimiter: String = ",",
    val indent: Int = 2,
    val strict: Boolean = true,
    val keyFolding: String = "off",
    val flattenDepth: Int? = null,
    val expandPaths: String = "off",
) {
    fun toToonConfiguration(): ToonConfiguration {
        return ToonConfiguration(
            delimiter = delimiter.toDelimiter(),
            indentSize = indent,
            strictMode = strict,
            keyFolding = keyFolding == "safe",
            pathExpansion = expandPaths == "safe",
        )
    }

    fun String.toDelimiter(): ToonConfiguration.Delimiter =
        when (this) {
            "," -> ToonConfiguration.Delimiter.COMMA
            "\t" -> ToonConfiguration.Delimiter.TAB
            "|" -> ToonConfiguration.Delimiter.PIPE
            else -> throw IllegalArgumentException("Unknown delimiter: $this")
        }
}