package com.lukelast.ktoon

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonToToonTest {

    @Test
    fun `test json to toon encoding`() {
        val jsonString = """
            {
                "name": "Alice",
                "age": 30,
                "isStudent": false,
                "address": {
                    "city": "Wonderland",
                    "zip": "12345"
                },
                "hobbies": ["reading", "chess"]
            }
        """.trimIndent()

        val toonString = Ktoon.Default.encodeJsonToToon(jsonString)

        val expectedToon = """
            name: Alice
            age: 30
            isStudent: false
            address:
              city: Wonderland
              zip: "12345"
            hobbies[2]: reading,chess
        """.trimIndent()

        assertEquals(expectedToon, toonString.trim())
    }
}

