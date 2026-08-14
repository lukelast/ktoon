package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Regression tests for the encoding nesting budget reported in `.workflow/issues`. */
class IssueJsonNestingTest {

    private val ktoon = Ktoon()

    private fun nestedArrays(levels: Int): JsonElement {
        var element: JsonElement = JsonPrimitive(1)
        repeat(levels) { element = JsonArray(listOf(element)) }
        return element
    }

    private fun nestedObjects(levels: Int): JsonElement {
        var element: JsonElement = JsonPrimitive(1)
        repeat(levels) { element = JsonObject(mapOf("a" to element)) }
        return element
    }

    @Test
    fun `a json tree at the nesting limit still encodes`() {
        ktoon.encodeJsonToToon(nestedArrays(DEFAULT_MAX_NESTING_DEPTH))
        ktoon.encodeJsonToToon(nestedObjects(DEFAULT_MAX_NESTING_DEPTH))
    }

    @Test
    fun `a json tree past the nesting limit is rejected`() {
        // SPEC §15: report a documented depth limit instead of exhausting the host stack.
        assertFailsWith<KtoonEncodingException> {
            ktoon.encodeJsonToToon(nestedArrays(DEFAULT_MAX_NESTING_DEPTH + 1))
        }
        assertFailsWith<KtoonEncodingException> {
            ktoon.encodeJsonToToon(nestedObjects(DEFAULT_MAX_NESTING_DEPTH + 1))
        }
    }

    @Test
    fun `deeply nested raw json is rejected before it is parsed`() {
        val deep = "[".repeat(10_000) + "1" + "]".repeat(10_000)
        assertFailsWith<KtoonEncodingException> { ktoon.encodeJsonToToon(deep) }
    }

    @Test
    fun `brackets inside json strings do not count towards the nesting limit`() {
        val brackets = "[".repeat(DEFAULT_MAX_NESTING_DEPTH + 10)
        ktoon.encodeJsonToToon("""{"a":"$brackets","b":"\"[["}""")
    }

    @Serializable data class Node(val child: Node? = null, val id: Int = 0)

    private fun nestedNodes(levels: Int): Node {
        var node = Node()
        repeat(levels - 1) { node = Node(child = node) }
        return node
    }

    @Test
    fun `a typed recursive value past the nesting limit is rejected`() {
        // The budget covers typed encoding too, not just the JsonElement bridge.
        ktoon.encodeToString(nestedNodes(DEFAULT_MAX_NESTING_DEPTH))
        assertFailsWith<KtoonEncodingException> {
            ktoon.encodeToString(nestedNodes(DEFAULT_MAX_NESTING_DEPTH + 1))
        }
    }

    @Test
    fun `a deeply recursive typed value fails with a library error`() {
        assertFailsWith<KtoonEncodingException> { ktoon.encodeToString(nestedNodes(10_000)) }
    }

    @Test
    fun `the encoder honors a configured nesting limit`() {
        val shallow = Ktoon { maxNestingDepth = 5 }
        shallow.encodeToString(nestedNodes(5))
        assertFailsWith<KtoonEncodingException> { shallow.encodeToString(nestedNodes(6)) }
        assertFailsWith<KtoonEncodingException> { shallow.encodeJsonToToon(nestedObjects(6)) }
    }

    @Test
    fun `a raised nesting limit round-trips values past the default`() {
        // The same limit on both sides keeps decode(encode(x)) possible at every setting.
        val deep = Ktoon { maxNestingDepth = DEFAULT_MAX_NESTING_DEPTH + 72 }
        val tree = nestedObjects(DEFAULT_MAX_NESTING_DEPTH + 72)
        assertEquals(tree, deep.decodeToonToJson(deep.encodeJsonToToon(tree)))
        assertFailsWith<KtoonEncodingException> { ktoon.encodeJsonToToon(tree) }
    }
}
