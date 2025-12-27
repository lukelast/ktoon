package com.lukelast.ktoon

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Sealed class for contextual nested in polymorphic test
@Serializable
sealed class Content {
    @Serializable @SerialName("text") data class Text(val value: String) : Content()

    @Serializable
    @SerialName("image")
    data class Image(val url: String, @Contextual val backgroundColor: KtoonContextualTest.Color) :
        Content()
}

/** Tests for contextual serialization using @Contextual and SerializersModule. */
class KtoonContextualTest {

    /** Simple ID class for KMP compatibility (replaces java.util.UUID). */
    data class Id(val value: String) {
        companion object {
            fun random(): Id = Id(Random.nextLong().toString(16) + Random.nextLong().toString(16))
        }
    }

    /** Custom serializer for Id. */
    object IdSerializer : KSerializer<Id> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Id", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Id) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): Id {
            return Id(decoder.decodeString())
        }
    }

    @Test
    fun `contextual serialization with passthrough serializer`() {
        @Serializable data class User(@Contextual val id: Id, val name: String)

        val module = SerializersModule { contextual(IdSerializer) }

        val ktoon = Ktoon(serializersModule = module)
        val id = Id("550e8400-e29b-41d4-a716-446655440000")
        val original = User(id, "Alice")

        val encoded = ktoon.encodeToString(original)

        // Verify the contextual serializer's output is in the encoded string
        assertTrue(encoded.contains("id:"), "Should contain 'id:' key")
        assertTrue(encoded.contains("550e8400-e29b-41d4-a716-446655440000"), "Should contain ID value")

        val decoded = ktoon.decodeFromString<User>(encoded)
        assertEquals(original, decoded)
    }

    /** Custom type that requires contextual serialization. */
    data class Color(val r: Int, val g: Int, val b: Int)

    object ColorSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Color) {
            encoder.encodeString("#%02x%02x%02x".format(value.r, value.g, value.b))
        }

        override fun deserialize(decoder: Decoder): Color {
            val hex = decoder.decodeString().removePrefix("#")
            return Color(
                r = hex.take(2).toInt(16),
                g = hex.substring(2, 4).toInt(16),
                b = hex.substring(4, 6).toInt(16),
            )
        }
    }

    @Test
    fun `contextual serialization with transforming serializer`() {
        @Serializable
        data class Theme(
            val name: String,
            @Contextual val primaryColor: Color,
            @Contextual val secondaryColor: Color,
        )

        val module = SerializersModule { contextual(ColorSerializer) }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Theme(
                name = "Dark Mode",
                primaryColor = Color(33, 33, 33),
                secondaryColor = Color(0, 150, 255),
            )

        val encoded = ktoon.encodeToString(original)

        // Verify format: Color(r,g,b) transforms to hex string "#rrggbb"
        assertTrue(encoded.contains("primaryColor:"), "Should contain 'primaryColor:' key")
        assertTrue(encoded.contains("#212121"), "Should contain hex color value")
        assertTrue(encoded.contains("secondaryColor:"), "Should contain 'secondaryColor:' key")
        assertTrue(encoded.contains("#0096ff"), "Should contain hex color value")

        val decoded = ktoon.decodeFromString<Theme>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual type annotation on list elements`() {
        @Serializable data class Palette(val name: String, val colors: List<@Contextual Color>)

        val module = SerializersModule { contextual(ColorSerializer) }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Palette(
                name = "Rainbow",
                colors =
                    listOf(
                        Color(255, 0, 0), // Red
                        Color(0, 128, 0), // Green
                        Color(0, 0, 255), // Blue
                    ),
            )

        val encoded = ktoon.encodeToString(original)

        // Verify format: each list element uses the contextual serializer
        assertTrue(encoded.contains("#ff0000"))
        assertTrue(encoded.contains("#008000"))
        assertTrue(encoded.contains("#0000ff"))

        val decoded = ktoon.decodeFromString<Palette>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual nullable field`() {
        @Serializable data class OptionalId(val name: String, @Contextual val id: Id?)

        val module = SerializersModule { contextual(IdSerializer) }

        val ktoon = Ktoon(serializersModule = module)

        // Test with value
        val withValue = OptionalId("Test", Id.random())
        val encodedWithValue = ktoon.encodeToString(withValue)
        val decodedWithValue = ktoon.decodeFromString<OptionalId>(encodedWithValue)
        assertEquals(withValue, decodedWithValue)

        // Test with null
        val withNull = OptionalId("Test", null)
        val encodedWithNull = ktoon.encodeToString(withNull)
        val decodedWithNull = ktoon.decodeFromString<OptionalId>(encodedWithNull)
        assertEquals(withNull, decodedWithNull)
    }

    @Test
    fun `multiple contextual types in module`() {
        @Serializable
        data class Document(
            @Contextual val id: Id,
            val title: String,
            @Contextual val backgroundColor: Color,
        )

        val module = SerializersModule {
            contextual(IdSerializer)
            contextual(ColorSerializer)
        }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Document(
                id = Id.random(),
                title = "My Document",
                backgroundColor = Color(255, 255, 255),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Document>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual types work with different Ktoon configurations`() {
        @Serializable
        data class Settings(
            @Contextual val sessionId: Id,
            @Contextual val theme: Color,
            val username: String,
        )

        val module = SerializersModule {
            contextual(IdSerializer)
            contextual(ColorSerializer)
        }

        val sessionId = Id("test-session-123")
        val original = Settings(sessionId = sessionId, theme = Color(0, 0, 0), username = "admin")

        // Default config
        val ktoonDefault = Ktoon(serializersModule = module)
        val encodedDefault = ktoonDefault.encodeToString(original)
        val decodedDefault = ktoonDefault.decodeFromString<Settings>(encodedDefault)
        assertEquals(original, decodedDefault)

        // Lenient config with different formatting
        val ktoonLenient =
            Ktoon(serializersModule = module) {
                strictMode = false
                indentSize = 4
            }
        val encodedLenient = ktoonLenient.encodeToString(original)
        val decodedLenient = ktoonLenient.decodeFromString<Settings>(encodedLenient)
        assertEquals(original, decodedLenient)
    }

    @Test
    fun `missing contextual serializer throws SerializerNotFound`() {
        @Serializable data class User(@Contextual val id: Id, val name: String)

        // Module without IdSerializer registered
        val module = SerializersModule { }
        val ktoon = Ktoon(serializersModule = module)

        val user = User(Id("test-id"), "Alice")

        assertFailsWith<kotlinx.serialization.SerializationException> {
            ktoon.encodeToString(user)
        }
    }

    @Test
    fun `malformed contextual value throws during deserialization`() {
        @Serializable
        data class Theme(@Contextual val color: Color)

        val module = SerializersModule { contextual(ColorSerializer) }
        val ktoon = Ktoon(serializersModule = module)

        // Malformed hex color (too short)
        val malformed = "color #12"

        assertFailsWith<Exception> {
            ktoon.decodeFromString<Theme>(malformed)
        }
    }
}
