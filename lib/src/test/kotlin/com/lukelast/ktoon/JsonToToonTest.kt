package com.lukelast.ktoon

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

