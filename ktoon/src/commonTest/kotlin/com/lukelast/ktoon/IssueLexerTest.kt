package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable

/** Regression tests for lexer/header issues reported in `.workflow/issues`. */
class IssueLexerTest {

    private val strict = Ktoon()

    @Serializable data class Named(val id: Int, val name: String)

    @Serializable data class GroupHolder(val group: Named)

    @Serializable data class GroupRoot(val items: List<GroupHolder>)

    @Test
    fun `a space before a nested field group's brace is not part of the field name`() {
        // §12: field names are extracted tokens, so surrounding spaces are trimmed.
        val input = "items[1]{group {id,name}}:\n  1,Ada"
        assertEquals(GroupRoot(listOf(GroupHolder(Named(1, "Ada")))), strict.decodeFromString(input))
    }
}
