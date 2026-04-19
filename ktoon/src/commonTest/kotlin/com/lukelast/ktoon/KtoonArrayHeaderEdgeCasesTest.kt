package com.lukelast.ktoon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Decoder tolerance tests for array-header edge cases in Section 6. These inputs are never emitted
 * by a conforming encoder, so they sit outside the roundtrip and fixture harnesses.
 */
class KtoonArrayHeaderEdgeCasesTest {

    private val ktoon = Ktoon()

    @Test
    fun `negative length falls through to literal key`() {
        @Serializable data class Root(@SerialName("foo[-3]") val v: Int)

        val decoded = ktoon.decodeFromString<Root>("foo[-3]: 42")
        assertEquals(Root(v = 42), decoded)
    }

    @Test
    fun `empty brackets fall through to literal key`() {
        @Serializable data class Root(@SerialName("foo[]") val v: String)

        val decoded = ktoon.decodeFromString<Root>("foo[]: hello")
        assertEquals(Root(v = "hello"), decoded)
    }

    @Test
    fun `positive signed length falls through to literal key`() {
        @Serializable data class Root(@SerialName("foo[+3]") val v: Int)

        val decoded = ktoon.decodeFromString<Root>("foo[+3]: 42")
        assertEquals(Root(v = 42), decoded)
    }

    @Test
    fun `positive signed length with delimiter marker falls through to literal key`() {
        @Serializable data class Root(@SerialName("foo[+2|]") val v: String)

        val decoded = ktoon.decodeFromString<Root>("foo[+2|]: hello")
        assertEquals(Root(v = "hello"), decoded)
    }

    @Test
    fun `whitespace between closing bracket and opening brace is allowed`() {
        @Serializable data class Item(val a: Int, val b: String)

        @Serializable data class Root(val items: List<Item>)

        val input = "items[2] {a,b}:\n  1,Ada\n  2,Bob"
        val expected = Root(items = listOf(Item(1, "Ada"), Item(2, "Bob")))
        assertEquals(expected, ktoon.decodeFromString<Root>(input))
    }

    @Test
    fun `whitespace between closing brace and colon is allowed`() {
        @Serializable data class Item(val a: Int, val b: String)

        @Serializable data class Root(val items: List<Item>)

        val input = "items[2]{a,b} :\n  1,Ada\n  2,Bob"
        val expected = Root(items = listOf(Item(1, "Ada"), Item(2, "Bob")))
        assertEquals(expected, ktoon.decodeFromString<Root>(input))
    }
}
