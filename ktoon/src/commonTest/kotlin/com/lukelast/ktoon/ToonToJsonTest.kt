package com.lukelast.ktoon

import com.lukelast.ktoon.serializers.KtoonJsonElementSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ToonToJsonTest {

    @Test
    fun `decodes a document to a JsonElement tree`() {
        val toon =
            """
            name: Alice
            age: 30
            isStudent: false
            address:
              city: Wonderland
              zip: "12345"
            hobbies[2]: reading,chess
            """
                .trimIndent()

        val expected = buildJsonObject {
            put("name", "Alice")
            put("age", 30)
            put("isStudent", false)
            putJsonObject("address") {
                put("city", "Wonderland")
                put("zip", "12345")
            }
            putJsonArray("hobbies") {
                add("reading")
                add("chess")
            }
        }

        assertEquals(expected, Ktoon.Default.decodeToonToJson(toon))
    }

    @Test
    fun `decodes root primitives and arrays`() {
        assertEquals(JsonPrimitive(42), Ktoon.Default.decodeToonToJson("42"))
        assertEquals(JsonPrimitive("hello"), Ktoon.Default.decodeToonToJson("hello"))
        assertEquals(
            buildJsonArray {
                add(1)
                add(2)
                add(3)
            },
            Ktoon.Default.decodeToonToJson("[3]: 1,2,3"),
        )
    }

    @Test
    fun `round-trips a JsonElement through TOON`() {
        val original = buildJsonObject {
            put("title", "report")
            put("count", 12)
            putJsonArray("rows") {
                add(buildJsonObject { put("a", 1) })
                add(buildJsonObject { put("a", 2) })
            }
        }

        val toon = Ktoon.Default.encodeJsonToToon(original)

        assertEquals(original, Ktoon.Default.decodeToonToJson(toon))
    }

    @Serializable
    data class Payload(
        val id: Int,
        @Serializable(with = KtoonJsonElementSerializer::class) val meta: JsonElement,
    )

    @Test
    fun `decodes a JsonElement field inside a typed class`() {
        val toon =
            """
            id: 7
            meta:
              active: true
              tags[2]: a,b
            """
                .trimIndent()

        val expectedMeta = buildJsonObject {
            put("active", true)
            putJsonArray("tags") {
                add("a")
                add("b")
            }
        }

        assertEquals(Payload(7, expectedMeta), Ktoon.Default.decodeFromString<Payload>(toon))
    }

    @Test
    fun `rejects non-Ktoon decoders with a clear error`() {
        assertFailsWith<KtoonDecodingException> {
            Json.decodeFromString(KtoonJsonElementSerializer, "{}")
        }
    }
}
