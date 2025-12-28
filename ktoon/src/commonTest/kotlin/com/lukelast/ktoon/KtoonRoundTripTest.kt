package com.lukelast.ktoon

import kotlinx.serialization.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/** Round-trip tests to verify that encoding and decoding preserve data integrity. */
class KtoonRoundTripTest {

    @Test
    fun `round trip simple object`() {
        @Serializable data class Person(val name: String, val age: Int)

        val ktoon = Ktoon()
        val original = Person("Alice", 30)

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Person>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip all primitive types`() {
        @Serializable
        data class AllPrimitives(
            val boolVal: Boolean,
            val byteVal: Byte,
            val shortVal: Short,
            val intVal: Int,
            val longVal: Long,
            val floatVal: Float,
            val doubleVal: Double,
            val charVal: Char,
            val stringVal: String,
        )

        val ktoon = Ktoon()
        val original =
            AllPrimitives(
                boolVal = true,
                byteVal = 42,
                shortVal = 1000,
                intVal = 123456,
                longVal = 9876543210L,
                floatVal = 3.14f,
                doubleVal = 2.71828,
                charVal = 'X',
                stringVal = "Hello TOON",
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<AllPrimitives>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip nested objects`() {
        @Serializable data class Address(val street: String, val city: String, val zip: Int)

        @Serializable data class User(val name: String, val address: Address)

        val ktoon = Ktoon()
        val original = User(name = "Bob", address = Address("123 Main St", "NYC", 10001))

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<User>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip primitive list`() {
        @Serializable data class TaggedItem(val name: String, val tags: List<String>)

        val ktoon = Ktoon()
        val original = TaggedItem("Item1", listOf("admin", "ops", "dev"))

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<TaggedItem>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip list of objects`() {
        @Serializable data class Person(val id: Int, val name: String)

        @Serializable data class Team(val name: String, val members: List<Person>)

        val ktoon = Ktoon()
        val original =
            Team(
                name = "Engineering",
                members = listOf(Person(1, "Alice"), Person(2, "Bob"), Person(3, "Charlie")),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Team>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip with null values`() {
        @Serializable data class Optional(val required: String, val optional: String?)

        val ktoon = Ktoon()
        val original = Optional("required value", null)

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Optional>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip with special characters in strings`() {
        @Serializable
        data class SpecialStrings(
            val empty: String,
            val withSpaces: String,
            val withQuotes: String,
            val withNewline: String,
        )

        val ktoon = Ktoon()
        val original =
            SpecialStrings(
                empty = "",
                withSpaces = "hello world",
                withQuotes = "She said \"hello\"",
                withNewline = "line1\nline2",
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<SpecialStrings>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip strings with colons`() {
        @Serializable data class ColonTest(val url: String, val items: List<String>)

        val ktoon = Ktoon()
        val original = ColonTest(url = "scheme:host:port", items = listOf("key:value", "a:b:c:d"))

        val encoded = ktoon.encodeToString(original)

        // Verify colons are quoted
        assertTrue(encoded.contains("\"scheme:host:port\""))

        // Verify round-trip preserves data
        val decoded = ktoon.decodeFromString<ColonTest>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `round trip deeply nested structure`() {
        @Serializable data class Level3(val value: String)

        @Serializable data class Level2(val data: Level3)

        @Serializable data class Level1(val nested: Level2)

        @Serializable data class Root(val top: Level1)

        val ktoon = Ktoon()
        val original = Root(top = Level1(nested = Level2(data = Level3("deep value"))))

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Root>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip empty collections`() {
        @Serializable data class WithEmpty(val name: String, val items: List<String>)

        val ktoon = Ktoon()
        val original = WithEmpty("test", emptyList())

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<WithEmpty>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip with custom configuration`() {
        @Serializable data class Person(val firstName: String, val lastName: String, val age: Int)

        val ktoon = Ktoon {
            strictMode = true
            indentSize = 4
        }

        val original = Person("John", "Doe", 25)

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Person>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip numbers with edge values`() {
        @Serializable
        data class EdgeNumbers(
            val zero: Int,
            val negativeZero: Double,
            val maxInt: Int,
            val minInt: Int,
            val decimal: Double,
            val wholeDouble: Double,
        )

        val ktoon = Ktoon()
        val original =
            EdgeNumbers(
                zero = 0,
                negativeZero = -0.0,
                maxInt = Int.MAX_VALUE,
                minInt = Int.MIN_VALUE,
                decimal = 1.5,
                wholeDouble = 2.0,
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<EdgeNumbers>(encoded)

        // Note: -0.0 normalizes to 0
        assertEquals(original.zero, decoded.zero)
        assertEquals(original.maxInt, decoded.maxInt)
        assertEquals(original.minInt, decoded.minInt)
        assertEquals(original.decimal, decoded.decimal, 0.0001)
        assertEquals(original.wholeDouble, decoded.wholeDouble, 0.0001)
    }
}
