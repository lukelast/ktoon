package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Spec 4.1 edge cases verified directly against the normative text and the reference
 * implementation, beyond what the fixture suite covers.
 */
class KtoonSpecV41EdgeCasesTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable data class TwoInts(val a: Int, val b: Int)

    @Test
    fun `crlf line endings are accepted`() {
        assertEquals(TwoInts(1, 2), strict.decodeFromString("a: 1\r\nb: 2"))
        assertEquals(TwoInts(1, 2), strict.decodeFromString("a: 1\r\nb: 2\r\n"))
    }

    @Serializable data class OneString(val key: String)

    @Test
    fun `a lone carriage return inside a line is content and not a line terminator`() {
        // §12: only a CR at the end of a line belongs to the CRLF terminator.
        assertEquals(OneString("a\rb"), strict.decodeFromString("key: a\rb"))
    }

    @Test
    fun `tab between key and bracket segment errors in strict mode and falls through in non-strict`() {
        @Serializable data class Root(@SerialName("foo\t[2]") val v: String)

        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("foo\t[2]: 1,2") }
        assertEquals(Root(v = "1,2"), lenient.decodeFromString<Root>("foo\t[2]: 1,2"))
    }

    @Test
    fun `content after a root scalar line errors in both modes`() {
        // §14.2: two or more depth-0 lines that are neither headers nor key-value lines are
        // invalid in strict and non-strict mode alike.
        assertFailsWith<KtoonException> { strict.decodeFromString<String>("hello\nworld") }
        assertFailsWith<KtoonException> { lenient.decodeFromString<String>("hello\nworld") }
    }

    @Test
    fun `inline content on a fields-bearing header errors in strict mode and falls through in non-strict`() {
        @Serializable data class Tabular(val items: List<TwoInts>)

        @Serializable data class Fallback(@SerialName("items[2]{a,b}") val v: String)

        assertFailsWith<KtoonException> { strict.decodeFromString<Tabular>("items[2]{a,b}: 1,2") }
        assertEquals(Fallback(v = "1,2"), lenient.decodeFromString<Fallback>("items[2]{a,b}: 1,2"))
    }

    @Serializable data class IntAndString(val a: Int, val b: String)

    @Test
    fun `header-shaped line at row depth with delimiter before colon is a row`() {
        @Serializable data class Root(val items: List<IntAndString>)

        // §5.2/§9.3: within a tabular scope the delimiter-before-colon rule is authoritative,
        // even for a line that happens to parse as an array header.
        val input = "items[2]{a,b}:\n  1,Ada\n  2,foo[2]: x"
        val expected = Root(listOf(IntAndString(1, "Ada"), IntAndString(2, "foo[2]: x")))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Test
    fun `keyed header with zero entries decodes to an empty object`() {
        @Serializable data class Root(val key: Map<String, IntAndString>)

        assertEquals(Root(emptyMap()), strict.decodeFromString("key[0:]{a,b}:"))
    }

    @Test
    fun `bare header with declared length one is list form and not one empty inline value`() {
        @Serializable data class Root(val key: List<String>)

        // §9.1: the single empty string is spelled `key[1]: ""`; a bare `key[1]:` is an array
        // in list form with zero items, which fails the strict count check.
        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("key[1]:") }
        assertEquals(Root(listOf("")), strict.decodeFromString("key[1]: \"\""))
    }

    @Test
    fun `row cell bracket pair token decodes as a string`() {
        @Serializable data class Root(val items: List<IntAndString>)

        // §9.3: the empty-array form does not apply inside rows; a `[]` cell is the string "[]".
        val input = "items[1]{a,b}:\n  1,\"[]\""
        assertEquals(Root(listOf(IntAndString(1, "[]"))), strict.decodeFromString(input))
        val unquoted = "items[1]{a,b}:\n  1,[]"
        assertEquals(Root(listOf(IntAndString(1, "[]"))), strict.decodeFromString(unquoted))
    }
}
