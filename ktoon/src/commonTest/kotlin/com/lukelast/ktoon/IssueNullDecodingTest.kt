package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull

/** Regression tests for null handling reported in `.workflow/issues`. */
class IssueNullDecodingTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable data class Defaults(val a: Int = 1, val b: String = "x")

    @Serializable data class StringField(val value: String)

    @Serializable data class MapField(val value: Map<String, String>)

    @Serializable data class ObjectField(val value: Defaults)

    @Serializable data class NullableObjectField(val value: Defaults?)

    @Test
    fun `a null does not become a fabricated non-null value`() {
        // A null is a value the document carried, not a missing one.
        for (ktoon in listOf(strict, lenient)) {
            assertFailsWith<KtoonException> { ktoon.decodeFromString<StringField>("value: null") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<MapField>("value: null") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<ObjectField>("value: null") }
        }
    }

    @Test
    fun `a null root does not become an empty structure`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<Defaults>("null") }
        assertFailsWith<KtoonException> { strict.decodeFromString<Map<String, String>>("null") }
        assertFailsWith<KtoonException> { strict.decodeFromString<List<String>>("null") }
    }

    @Test
    fun `nullable fields and quoted null still decode`() {
        assertEquals(NullableObjectField(null), strict.decodeFromString("value: null"))
        assertEquals(StringField("null"), strict.decodeFromString("value: \"null\""))
    }

    @Test
    fun `schemaless decoding still yields JsonNull`() {
        assertEquals(JsonNull, strict.decodeToonToJson("null"))
    }
}
