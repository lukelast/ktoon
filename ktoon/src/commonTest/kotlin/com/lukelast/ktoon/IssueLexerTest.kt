package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

/** Regression tests for lexer/header issues reported in `.workflow/issues`. */
class IssueLexerTest {

    private val strict = Ktoon()

    @Serializable data class Named(val id: Int, val name: String)

    @Serializable data class GroupHolder(val group: Named)

    @Serializable data class GroupRoot(val items: List<GroupHolder>)

    @Serializable data class TwoStrings(val a: String, val b: String)

    @Test
    fun `a tab-delimited row keeps an empty leading cell`() {
        // §11.2: splitting preserves empty tokens, so the row's leading tab is the delimiter and
        // not indentation.
        val input = "[2\t]{a\tb}:\n  \tone\n  two\tthree"
        val expected = listOf(TwoStrings("", "one"), TwoStrings("two", "three"))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Test
    fun `a tab-delimited row of only empty cells is a row and not a blank line`() {
        val input = "[1\t]{a\tb}:\n  \t"
        assertEquals(listOf(TwoStrings("", "")), strict.decodeFromString(input))
    }

    @Test
    fun `a tab used as indentation is still an error`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<TwoStrings>("\ta: 1\nb: 2") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, String>>("a:\n  \tb: 1")
        }
    }

    @Test
    fun `a space before a nested field group's brace is not part of the field name`() {
        // §12: field names are extracted tokens, so surrounding spaces are trimmed.
        val input = "items[1]{group {id,name}}:\n  1,Ada"
        assertEquals(
            GroupRoot(listOf(GroupHolder(Named(1, "Ada")))),
            strict.decodeFromString(input),
        )
    }
}
