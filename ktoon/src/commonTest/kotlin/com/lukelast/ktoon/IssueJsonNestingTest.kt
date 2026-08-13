package com.lukelast.ktoon

import com.lukelast.ktoon.serializers.MAX_JSON_NESTING_DEPTH
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Regression tests for the JSON encoding nesting budget reported in `.workflow/issues`. */
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
        ktoon.encodeJsonToToon(nestedArrays(MAX_JSON_NESTING_DEPTH))
        ktoon.encodeJsonToToon(nestedObjects(MAX_JSON_NESTING_DEPTH))
    }

    @Test
    fun `a json tree past the nesting limit is rejected`() {
        // SPEC §15: report a documented depth limit instead of exhausting the host stack.
        assertFailsWith<KtoonEncodingException> {
            ktoon.encodeJsonToToon(nestedArrays(MAX_JSON_NESTING_DEPTH + 1))
        }
        assertFailsWith<KtoonEncodingException> {
            ktoon.encodeJsonToToon(nestedObjects(MAX_JSON_NESTING_DEPTH + 1))
        }
    }

    @Test
    fun `deeply nested raw json is rejected before it is parsed`() {
        val deep = "[".repeat(10_000) + "1" + "]".repeat(10_000)
        assertFailsWith<KtoonEncodingException> { ktoon.encodeJsonToToon(deep) }
    }

    @Test
    fun `brackets inside json strings do not count towards the nesting limit`() {
        val brackets = "[".repeat(MAX_JSON_NESTING_DEPTH + 10)
        ktoon.encodeJsonToToon("""{"a":"$brackets","b":"\"[["}""")
    }
}
