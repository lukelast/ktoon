package com.lukelast.ktoon

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KtoonEncodingTest {

    @Test
    fun `encode simple object with primitives`() {
        @Serializable data class Person(val name: String, val age: Int)

        val ktoon = Ktoon()
        val person = Person("Alice", 30)
        val encoded = ktoon.encodeToString(person)

        // Verify it contains the expected keys and values
        assertTrue(encoded.contains("name:"))
        assertTrue(encoded.contains("Alice"))
        assertTrue(encoded.contains("age:"))
        assertTrue(encoded.contains("30"))
    }

    @Test
    fun `encode object with different primitive types`() {
        @Serializable
        data class AllTypes(
            val stringVal: String,
            val intVal: Int,
            val longVal: Long,
            val doubleVal: Double,
            val boolVal: Boolean,
            val nullVal: String?,
        )

        val ktoon = Ktoon()
        val obj =
            AllTypes(
                stringVal = "hello",
                intVal = 42,
                longVal = 123456789L,
                doubleVal = 3.14,
                boolVal = true,
                nullVal = null,
            )

        val encoded = ktoon.encodeToString(obj)

        assertTrue(encoded.contains("stringVal:"))
        assertTrue(encoded.contains("hello"))
        assertTrue(encoded.contains("intVal:"))
        assertTrue(encoded.contains("42"))
        assertTrue(encoded.contains("doubleVal:"))
        assertTrue(encoded.contains("3.14"))
        assertTrue(encoded.contains("boolVal:"))
        assertTrue(encoded.contains("true"))
        assertTrue(encoded.contains("nullVal:"))
        assertTrue(encoded.contains("null"))
    }

    @Test
    fun `encode primitive array inline format`() {
        @Serializable data class TaggedItem(val name: String, val tags: List<String>)

        val ktoon = Ktoon()
        val item = TaggedItem("Item1", listOf("admin", "ops", "dev"))

        val encoded = ktoon.encodeToString(item)

        assertTrue(encoded.contains("tags[3]:"))
        assertTrue(encoded.contains("admin"))
        assertTrue(encoded.contains("ops"))
        assertTrue(encoded.contains("dev"))
    }

    @Test
    fun `encode numbers with normalization`() {
        @Serializable data class Numbers(val int: Int, val float: Float, val double: Double)

        val ktoon = Ktoon()
        val nums = Numbers(int = 100, float = 1.5f, double = 2.0)

        val encoded = ktoon.encodeToString(nums)

        // Numbers should be normalized (no trailing zeros for 2.0 -> should be 2)
        assertTrue(encoded.contains("int: 100"))
        assertTrue(encoded.contains("float: 1.5"))
        // Double 2.0 should be normalized to "2"
        assertTrue(encoded.contains("double: 2"))
    }

    @Test
    fun `encode nested object`() {
        @Serializable data class Address(val city: String, val zip: Int)

        @Serializable data class User(val name: String, val address: Address)

        val ktoon = Ktoon()
        val user = User("Bob", Address("NYC", 10001))

        val encoded = ktoon.encodeToString(user)

        assertTrue(encoded.contains("name:"))
        assertTrue(encoded.contains("Bob"))
        assertTrue(encoded.contains("address:"))
        assertTrue(encoded.contains("city:"))
        assertTrue(encoded.contains("NYC"))
        assertTrue(encoded.contains("zip:"))
        assertTrue(encoded.contains("10001"))
    }

    @Test
    fun `encode string with special characters requires quoting`() {
        @Serializable
        data class SpecialStrings(
            val empty: String,
            val withSpace: String,
            val keyword: String,
            val withColon: String,
        )

        val ktoon = Ktoon()
        val obj =
            SpecialStrings(
                empty = "",
                withSpace = "hello world",
                keyword = "null",
                withColon = "key:value",
            )

        val encoded = ktoon.encodeToString(obj)

        // Empty strings and special cases should be quoted
        assertTrue(encoded.contains("\"\"")) // empty string
        assertTrue(encoded.contains("\"hello world\"") || encoded.contains("hello world")) // spaces
        assertTrue(encoded.contains("\"null\"")) // keyword
        // Colons must always be quoted per spec section 7.2
        assertTrue(encoded.contains("\"key:value\""))
    }

    @Test
    fun `encode strings with colons in all contexts`() {
        @Serializable data class Address(val location: String)

        @Serializable
        data class ColonContexts(val url: String, val paths: List<String>, val nested: Address)

        val ktoon = Ktoon()
        val obj =
            ColonContexts(
                url = "http://example.com:8080",
                paths = listOf("key:value", "a:b:c", "normal"),
                nested = Address("city:state"),
            )

        val encoded = ktoon.encodeToString(obj)

        // All strings containing colons must be quoted
        assertTrue(encoded.contains("\"http://example.com:8080\""))
        assertTrue(encoded.contains("\"key:value\""))
        assertTrue(encoded.contains("\"a:b:c\""))
        assertTrue(encoded.contains("\"city:state\""))
        // String without colon should not be quoted unnecessarily
        assertFalse(encoded.contains("\"normal\""))
    }

    @Test
    fun `encode with custom configuration`() {
        @Serializable data class Person(val name: String, val age: Int)

        val ktoon = Ktoon {
            strictMode = true
            indentSize = 4
        }

        val person = Person("Charlie", 25)
        val encoded = ktoon.encodeToString(person)

        assertTrue(encoded.contains("name:"))
        assertTrue(encoded.contains("Charlie"))
    }
}
