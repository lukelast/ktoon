package com.lukelast.ktoon

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SortedFieldsEncodeTest {

    @Test
    fun `sorts fields alphabetically when enabled`() {
        @Serializable
        data class User(val id: Int, val name: String, val active: Boolean)

        val user = User(1, "Alice", true)

        val config = KtoonConfiguration(sortFields = true)
        val toon = Ktoon(configuration = config)
        val encoded = toon.encodeToString(user)

        val expected = """
            active: true
            id: 1
            name: Alice
        """.trimIndent()

        Assertions.assertEquals(expected, encoded.trim())
    }

    @Test
    fun `preserves order when sortFields is false`() {
        @Serializable
        data class User(val id: Int, val name: String, val active: Boolean)

        val user = User(1, "Alice", true)

        val config = KtoonConfiguration(sortFields = false)
        val toon = Ktoon(configuration = config)
        val encoded = toon.encodeToString(user)

        val expected = """
            id: 1
            name: Alice
            active: true
        """.trimIndent()

        Assertions.assertEquals(expected, encoded.trim())
    }

    @Test
    fun `sorts nested objects`() {
        @Serializable
        data class Address(val zip: String, val city: String)
        @Serializable
        data class User(val name: String, val address: Address)

        val user = User("Alice", Address("12345", "Wonderland"))

        val config = KtoonConfiguration(sortFields = true)
        val toon = Ktoon(configuration = config)
        val encoded = toon.encodeToString(user)

        // address comes before name
        // city comes before zip
        val expected = """
            address:
              city: Wonderland
              zip: "12345"
            name: Alice
        """.trimIndent()

        Assertions.assertEquals(expected, encoded.trim())
    }
}