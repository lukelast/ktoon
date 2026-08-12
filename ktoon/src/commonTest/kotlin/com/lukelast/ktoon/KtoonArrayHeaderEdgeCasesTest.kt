package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Array-header edge cases from Section 6. Since spec 4.x, malformed bracket segments and stray
 * whitespace inside a header are strict-mode errors; the key-value fall-through only applies in
 * non-strict mode. Conforming encoders never emit these inputs.
 */
class KtoonArrayHeaderEdgeCasesTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Test
    fun `negative length errors in strict mode and falls through to literal key in non-strict`() {
        @Serializable data class Root(@SerialName("foo[-3]") val v: Int)

        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("foo[-3]: 42") }
        assertEquals(Root(v = 42), lenient.decodeFromString<Root>("foo[-3]: 42"))
    }

    @Test
    fun `empty brackets error in strict mode and fall through to literal key in non-strict`() {
        @Serializable data class Root(@SerialName("foo[]") val v: String)

        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("foo[]: hello") }
        assertEquals(Root(v = "hello"), lenient.decodeFromString<Root>("foo[]: hello"))
    }

    @Test
    fun `positive signed length errors in strict mode and falls through to literal key in non-strict`() {
        @Serializable data class Root(@SerialName("foo[+3]") val v: Int)

        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("foo[+3]: 42") }
        assertEquals(Root(v = 42), lenient.decodeFromString<Root>("foo[+3]: 42"))
    }

    @Test
    fun `positive signed length with delimiter marker errors in strict mode and falls through in non-strict`() {
        @Serializable data class Root(@SerialName("foo[+2|]") val v: String)

        assertFailsWith<KtoonException> { strict.decodeFromString<Root>("foo[+2|]: hello") }
        assertEquals(Root(v = "hello"), lenient.decodeFromString<Root>("foo[+2|]: hello"))
    }

    @Test
    fun `whitespace between closing bracket and opening brace errors in strict mode`() {
        @Serializable data class Item(val a: Int, val b: String)

        @Serializable data class Root(val items: List<Item>)

        val input = "items[2] {a,b}:\n  1,Ada\n  2,Bob"
        assertFailsWith<KtoonException> { strict.decodeFromString<Root>(input) }
    }

    @Test
    fun `whitespace between closing brace and colon errors in strict mode`() {
        @Serializable data class Item(val a: Int, val b: String)

        @Serializable data class Root(val items: List<Item>)

        val input = "items[2]{a,b} :\n  1,Ada\n  2,Bob"
        assertFailsWith<KtoonException> { strict.decodeFromString<Root>(input) }
    }
}
