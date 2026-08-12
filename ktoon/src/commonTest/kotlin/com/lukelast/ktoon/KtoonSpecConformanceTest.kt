package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable

/** Direct checks of normative SPEC.md requirements beyond the fixture suite. */
class KtoonSpecConformanceTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable
    data class NonFiniteNumbers(
        val nanDouble: Double,
        val positiveDouble: Double,
        val negativeDouble: Double,
        val nanFloat: Float,
        val positiveFloat: Float,
        val negativeFloat: Float,
    )

    @Test
    fun `non-finite host numbers normalize to null`() {
        val value =
            NonFiniteNumbers(
                nanDouble = Double.NaN,
                positiveDouble = Double.POSITIVE_INFINITY,
                negativeDouble = Double.NEGATIVE_INFINITY,
                nanFloat = Float.NaN,
                positiveFloat = Float.POSITIVE_INFINITY,
                negativeFloat = Float.NEGATIVE_INFINITY,
            )

        assertEquals(
            """
            nanDouble: null
            positiveDouble: null
            negativeDouble: null
            nanFloat: null
            positiveFloat: null
            negativeFloat: null
            """
                .trimIndent(),
            strict.encodeToString(value),
        )
    }

    @Serializable
    data class NumericExtremes(
        val minLong: Long,
        val maxLong: Long,
        val belowCanonicalRange: Double,
        val atUpperCanonicalBoundary: Double,
    )

    @Test
    fun `numeric domain extremes retain precision through a round trip`() {
        val value = NumericExtremes(Long.MIN_VALUE, Long.MAX_VALUE, 1e-7, 1e21)
        val encoded = strict.encodeToString(value)

        assertTrue(encoded.contains("minLong: -9223372036854775808"))
        assertTrue(encoded.contains("maxLong: 9223372036854775807"))
        assertEquals(value, strict.decodeFromString(encoded))
    }

    @Test
    fun `unpaired host surrogates are rejected during encoding`() {
        assertFailsWith<KtoonEncodingException> { strict.encodeToString("\uD800") }
        assertFailsWith<KtoonEncodingException> { strict.encodeToString("\uDC00") }
    }

    @Test
    fun `non-strict inline array count mismatch preserves every value`() {
        assertEquals(listOf(1, 2, 3), lenient.decodeFromString<List<Int>>("[1]: 1,2,3"))
        assertEquals(listOf(1), lenient.decodeFromString<List<Int>>("[3]: 1"))
    }

    @Test
    fun `non-strict tabular width mismatch omits missing fields and ignores surplus cells`() {
        val expected = listOf(mapOf("a" to 1), mapOf("a" to 2, "b" to 3))
        val input = "[2]{a,b}:\n  1\n  2,3,4"

        assertEquals(expected, lenient.decodeFromString<List<Map<String, Int>>>(input))
    }

    @Test
    fun `non-strict keyed count and width mismatches preserve all entries`() {
        val expected =
            linkedMapOf(
                "first" to mapOf("a" to 1),
                "second" to mapOf("a" to 2, "b" to 3),
            )
        val input = "[1:]{a,b}:\n  first: 1\n  second: 2,3,4"

        assertEquals(expected, lenient.decodeFromString<Map<String, Map<String, Int>>>(input))
    }

    @Test
    fun `leading and trailing empty cells are preserved in every delimited form`() {
        assertEquals(
            listOf("", "middle", ""),
            strict.decodeFromString<List<String>>("[3]: ,middle,"),
        )
        assertEquals(
            listOf(mapOf("a" to "", "b" to "middle", "c" to "")),
            strict.decodeFromString<List<Map<String, String>>>("[1]{a,b,c}:\n  ,middle,"),
        )
        assertEquals(
            mapOf("row" to mapOf("a" to "", "b" to "middle", "c" to "")),
            strict.decodeFromString<Map<String, Map<String, String>>>(
                "[1:]{a,b,c}:\n  row: ,middle,"
            ),
        )
    }

    @Test
    fun `all surrogate escapes are rejected including an encoded surrogate pair`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<String>("\"\\uDC00\"") }
        assertFailsWith<KtoonException> { strict.decodeFromString<String>("\"\\uD83D\\uDE80\"") }
    }

    @Test
    fun `literal control characters in quoted strings are rejected in every mode`() {
        for (ktoon in listOf(strict, lenient)) {
            assertFailsWith<KtoonException> { ktoon.decodeFromString<String>("\"a\u0004b\"") }
        }
    }

    @Test
    fun `non-hex unicode escape is rejected`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<String>("\"\\u12G4\"") }
        // A signed hex token is not 4HEXDIG even though a host integer parser would accept it.
        assertFailsWith<KtoonException> { strict.decodeFromString<String>("\"\\u+123\"") }
    }

    @Test
    fun `characters after quoted keys and field names are rejected in every mode`() {
        for (ktoon in listOf(strict, lenient)) {
            assertFailsWith<KtoonException> {
                ktoon.decodeFromString<Map<String, Int>>("\"a\"x: 1")
            }
            assertFailsWith<KtoonException> {
                ktoon.decodeFromString<Map<String, List<Int>>>("\"a\"x[1]: 1")
            }
            assertFailsWith<KtoonException> {
                ktoon.decodeFromString<List<Map<String, Int>>>("[0]{\"a\"x}:")
            }
        }
    }

    @Test
    fun `number recognition is limited to ASCII digits`() {
        assertEquals("١٢", strict.decodeFromString<String>("١٢"))
        assertEquals("１２", strict.decodeFromString<String>("１２"))
        assertEquals("١٢", strict.encodeToString("١٢"))
        assertEquals("１２", strict.encodeToString("１２"))
    }

    @Test
    fun `valid zero-row tabular headers decode to empty arrays`() {
        @Serializable data class Root(val items: List<Map<String, Int>>)

        assertEquals(Root(emptyList()), strict.decodeFromString("items[0]{a}:"))
        assertEquals(emptyList(), strict.decodeFromString<List<Map<String, Int>>>("[0]{a}:"))
    }

    @Test
    fun `field names may repeat at different nesting levels`() {
        @Serializable data class Nested(val x: Int)
        @Serializable data class Row(val x: Int, val nested: Nested)

        assertEquals(
            listOf(Row(1, Nested(2))),
            strict.decodeFromString<List<Row>>("[1]{x,nested{x}}:\n  1,2"),
        )
    }

    @Test
    fun `non-ASCII whitespace at the start of a root token is content`() {
        assertEquals("\u00A0hello", strict.decodeFromString<String>("\u00A0hello"))
        assertEquals("\u00A0", strict.decodeFromString<String>("\u00A0"))
        assertEquals(
            mapOf("\u00A0key" to 1),
            strict.decodeFromString<Map<String, Int>>("\u00A0key: 1"),
        )
    }

    @Test
    fun `a leading hyphen is ordinary scalar or key content outside a list scope`() {
        assertEquals("- item", strict.decodeFromString<String>("- item"))
        assertEquals("-", strict.decodeFromString<String>("-"))
        assertEquals(
            mapOf("- key" to 1),
            strict.decodeFromString<Map<String, Int>>("- key: 1"),
        )
    }

    @Test
    fun `byte-order mark away from the start of the document is content`() {
        assertEquals(
            mapOf("value" to "\uFEFFx"),
            strict.decodeFromString<Map<String, String>>("value: \uFEFFx"),
        )
    }

    @Test
    fun `nested field groups reject a delimiter different from the header delimiter`() {
        val input = "[1|]{a|nested{x,y}}:\n  1|2|3"

        assertFailsWith<KtoonException> { strict.decodeFromString<List<Map<String, Int>>>(input) }
    }

    @Test
    fun `root primitive quoting uses the configured document delimiter`() {
        val pipe = Ktoon { delimiter = KtoonConfiguration.Delimiter.PIPE }

        assertEquals("\"left|right\"", pipe.encodeToString("left|right"))
        assertEquals("left,right", pipe.encodeToString("left,right"))
    }

    @Test
    fun `duplicate decoded keys keep their first document position in non-strict mode`() {
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, Int>>("a: 1\n\"\\u0061\": 2")
        }

        // §14.3 last-write-wins picks the value; §2's equality rule makes key order observable,
        // and the reference implementation keeps the key's first document position.
        val decoded = lenient.decodeFromString<Map<String, Int>>("a: 1\nb: 2\n\"\\u0061\": 3")

        assertEquals(listOf("a", "b"), decoded.keys.toList())
        assertEquals(mapOf("a" to 3, "b" to 2), decoded)
    }
}
