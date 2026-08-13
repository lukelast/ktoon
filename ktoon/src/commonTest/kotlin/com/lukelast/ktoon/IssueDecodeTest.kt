package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

/** Regression tests for decoding issues reported in `.workflow/issues`. */
class IssueDecodeTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable data class OneString(val key: String)

    @Test
    fun `a literal unpaired surrogate is rejected while decoding`() {
        // §7.1: `unescaped-char` excludes U+D800–U+DFFF, and the encoder rejects such strings,
        // so accepting one on decode would produce a value that cannot be encoded again.
        val lone = "\uD800"
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: \"a${lone}b\"") }
        assertFailsWith<KtoonException> { lenient.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("a${lone}b: v") }
    }

    @Test
    fun `a well-formed surrogate pair still decodes`() {
        assertEquals(OneString("a😀b"), strict.decodeFromString("key: a😀b"))
    }

    @Serializable data class OneValue(val value: String)

    @Test
    fun `a dash-prefixed tabular row is a row and not a list item`() {
        // §5.2: outside a list scope a leading hyphen has no structural meaning.
        val rows = listOf(OneValue("- x"), OneValue("- y"))
        assertEquals(rows, strict.decodeFromString("[2]{value}:\n  - x\n  - y"))
    }

    @Serializable data class TwoInts(val a: Int, val b: Int)

    @Serializable data class BlankItem(val a: Map<String, String> = emptyMap(), val b: Int = 0)

    @Serializable data class BlankItems(val items: List<BlankItem>)

    @Serializable data class NestedTable(val items: List<TableItem>)

    @Serializable data class TableItem(val t: List<TwoInts>)

    @Serializable data class TableRoot(val items: List<TwoInts>)

    @Test
    fun `a blank line inside a started list item span errors in strict mode`() {
        // §12: the span starts at the item line, so a blank between a bare field and the next
        // sibling is inside it even though the item's own content had not begun.
        val input = "items[1]:\n  - a:\n\n    b: 1"
        assertFailsWith<KtoonException> { strict.decodeFromString<BlankItems>(input) }
        assertEquals(
            BlankItems(listOf(BlankItem(b = 1))),
            lenient.decodeFromString<BlankItems>(input),
        )
    }

    @Test
    fun `a blank line before a nested table's first row errors inside a started span`() {
        val input = "items[1]:\n  - t[2]{a,b}:\n\n      1,2\n      3,4"
        assertFailsWith<KtoonException> { strict.decodeFromString<NestedTable>(input) }
    }

    @Test
    fun `a blank line before a root header's first row is still accepted`() {
        // §12: blanks between a header and its scope's first row are ignored, not span errors.
        val input = "items[2]{a,b}:\n\n  1,2\n  3,4"
        val expected = TableRoot(listOf(TwoInts(1, 2), TwoInts(3, 4)))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Serializable data class TableAndField(val items: List<TwoInts>, val other: Int)

    @Test
    fun `a blank line after a scope's content is still accepted`() {
        val input = "items[1]{a,b}:\n  1,2\n\nother: 5"
        assertEquals(
            TableAndField(listOf(TwoInts(1, 2)), 5),
            strict.decodeFromString<TableAndField>(input),
        )
    }

    @Serializable data class OneInt(val value: Int)

    @Test
    fun `a dash-prefixed keyed entry row keeps the hyphen in its entry key`() {
        val expected = mapOf("- key" to OneInt(1), "other" to OneInt(2))
        assertEquals(expected, strict.decodeFromString("[2:]{value}:\n  - key: 1\n  other: 2"))
    }
}
