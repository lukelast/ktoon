package com.lukelast.ktoon


import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.assertEquals
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

/** Tests for custom serializer support. */
class KtoonCustomSerializerTest {

    /** Local date class for KMP compatibility testing. */
    data class SimpleDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String =
            "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    /** Custom serializer for SimpleDate that formats as ISO-8601 string. */
    object SimpleDateSerializer : KSerializer<SimpleDate> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("SimpleDate", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SimpleDate) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): SimpleDate {
            val parts = decoder.decodeString().split("-")
            return SimpleDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }

    @Test
    fun `custom serializer for date type`() {
        @Serializable
        data class Event(
            val name: String,
            @Serializable(with = SimpleDateSerializer::class) val date: SimpleDate,
        )

        val ktoon = Ktoon()
        val original = Event("Birthday", SimpleDate(2024, 12, 25))

        val encoded = ktoon.encodeToString(original)

        // Should contain ISO date format
        assertTrue(encoded.contains("2024-12-25"))

        val decoded = ktoon.decodeFromString<Event>(encoded)
        assertEquals(original, decoded)
    }

    /** Custom serializer for coordinate pairs. */
    @Serializable(with = CoordinateSerializer::class)
    data class Coordinate(val x: Double, val y: Double)

    object CoordinateSerializer : KSerializer<Coordinate> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Coordinate", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Coordinate) {
            encoder.encodeString("${value.x},${value.y}")
        }

        override fun deserialize(decoder: Decoder): Coordinate {
            val parts = decoder.decodeString().split(",")
            return Coordinate(parts[0].toDouble(), parts[1].toDouble())
        }
    }

    @Test
    fun `custom serializer on class level`() {
        @Serializable data class Location(val name: String, val coord: Coordinate)

        val ktoon = Ktoon()
        val original = Location("Home", Coordinate(40.7128, -74.0060))

        val encoded = ktoon.encodeToString(original)

        // Should be encoded as string "x,y"
        assertTrue(encoded.contains("40.7128,-74.006"))

        val decoded = ktoon.decodeFromString<Location>(encoded)
        assertEquals(original, decoded)
    }

    /** Custom serializer for money amounts (cents as Int). */
    object MoneySerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Money", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Int) {
            val dollars = value / 100
            val cents = value % 100
            encoder.encodeString("$${dollars}.${cents.toString().padStart(2, '0')}")
        }

        override fun deserialize(decoder: Decoder): Int {
            val str = decoder.decodeString().removePrefix("$")
            val parts = str.split(".")
            return parts[0].toInt() * 100 + parts[1].toInt()
        }
    }

    @Test
    fun `custom serializer for formatted money`() {
        @Serializable
        data class Product(
            val name: String,
            @Serializable(with = MoneySerializer::class) val priceCents: Int,
        )

        val ktoon = Ktoon()
        val original = Product("Coffee", 350) // $3.50

        val encoded = ktoon.encodeToString(original)

        // Should contain formatted money
        assertTrue(encoded.contains("$3.50"))

        val decoded = ktoon.decodeFromString<Product>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `multiple custom serializers`() {
        @Serializable
        data class Meeting(
            val title: String,
            @Serializable(with = SimpleDateSerializer::class) val date: SimpleDate,
            val location: Coordinate,
            @Serializable(with = MoneySerializer::class) val budgetCents: Int,
        )

        val ktoon = Ktoon()
        val original =
            Meeting(
                title = "Annual Conference",
                date = SimpleDate(2025, 6, 15),
                location = Coordinate(51.5074, -0.1278),
                budgetCents = 500000, // $5000.00
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Meeting>(encoded)
        assertEquals(original, decoded)
    }

    /** Custom serializer for lists that encodes as delimited string. */
    object StringListSerializer : KSerializer<List<String>> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("StringList", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: List<String>) {
            encoder.encodeString(value.joinToString("|"))
        }

        override fun deserialize(decoder: Decoder): List<String> {
            return decoder.decodeString().split("|")
        }
    }

    @Test
    fun `custom serializer for collection`() {
        @Serializable
        data class Config(
            val name: String,
            @Serializable(with = StringListSerializer::class) val tags: List<String>,
        )

        val ktoon = Ktoon()
        val original = Config("Server Config", listOf("prod", "web", "east"))

        val encoded = ktoon.encodeToString(original)

        // Should be pipe-delimited string
        assertTrue(encoded.contains("prod|web|east"))

        val decoded = ktoon.decodeFromString<Config>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    @Ignore
    fun `round trip with nested custom serializers`() {
        @Serializable
        data class Schedule(
            val events: List<@Serializable(with = SimpleDateSerializer::class) SimpleDate>
        )

        val ktoon = Ktoon()
        val original =
            Schedule(
                events =
                    listOf(
                        SimpleDate(2025, 1, 1),
                        SimpleDate(2025, 6, 15),
                        SimpleDate(2025, 12, 31),
                    )
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Schedule>(encoded)
        assertEquals(original, decoded)
    }
}
