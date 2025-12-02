package com.lukelast.ktoon

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/** Tests for custom serializer support. */
class KtoonCustomSerializerTest {

    /** Custom serializer for LocalDate that formats as ISO-8601. */
    object LocalDateSerializer : KSerializer<LocalDate> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: LocalDate) {
            encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }

        override fun deserialize(decoder: Decoder): LocalDate {
            return LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    @Test
    fun `custom serializer for date type`() {
        @Serializable
        data class Event(
            val name: String,
            @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
        )

        val ktoon = Ktoon()
        val original = Event("Birthday", LocalDate.of(2024, 12, 25))

        val encoded = ktoon.encodeToString(original)

        // Should contain ISO date format
        assert(encoded.contains("2024-12-25"))

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
        assert(encoded.contains("40.7128,-74.006"))

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
            encoder.encodeString("$${dollars}.%02d".format(cents))
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
        assert(encoded.contains("$3.50"))

        val decoded = ktoon.decodeFromString<Product>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `multiple custom serializers`() {
        @Serializable
        data class Meeting(
            val title: String,
            @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
            val location: Coordinate,
            @Serializable(with = MoneySerializer::class) val budgetCents: Int,
        )

        val ktoon = Ktoon()
        val original =
            Meeting(
                title = "Annual Conference",
                date = LocalDate.of(2025, 6, 15),
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
        assert(encoded.contains("prod|web|east"))

        val decoded = ktoon.decodeFromString<Config>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    @Disabled
    fun `round trip with nested custom serializers`() {
        @Serializable
        data class Schedule(
            val events: List<@Serializable(with = LocalDateSerializer::class) LocalDate>
        )

        val ktoon = Ktoon()
        val original =
            Schedule(
                events =
                    listOf(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 6, 15),
                        LocalDate.of(2025, 12, 31),
                    )
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Schedule>(encoded)
        assertEquals(original, decoded)
    }
}
