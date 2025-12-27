package com.lukelast.ktoon

import java.util.*
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
import kotlin.test.assertEquals
import kotlin.test.Ignore
import kotlin.test.Test

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

    /** Custom serializer for UUID. */
    object UUIDSerializer : KSerializer<UUID> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): UUID {
            return UUID.fromString(decoder.decodeString())
        }
    }

    @Test
    fun `contextual serialization with UUID`() {
        @Serializable data class User(@Contextual val id: UUID, val name: String)

        val module = SerializersModule { contextual(UUIDSerializer) }

        val ktoon = Ktoon(serializersModule = module)
        val uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val original = User(uuid, "Alice")

        val encoded = ktoon.encodeToString(original)

        assert(encoded.contains("550e8400-e29b-41d4-a716-446655440000"))

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
    fun `contextual serialization with custom type`() {
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

        assert(encoded.contains("#212121"))
        assert(encoded.contains("#0096ff"))

        val decoded = ktoon.decodeFromString<Theme>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual list serialization`() {
        @Serializable data class Palette(val name: String, val colors: List<@Contextual Color>)

        val module = SerializersModule { contextual(ColorSerializer) }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Palette(
                name = "Rainbow",
                colors =
                    listOf(
                        Color(255, 0, 0), // Red
                        Color(255, 165, 0), // Orange
                        Color(255, 255, 0), // Yellow
                        Color(0, 128, 0), // Green
                        Color(0, 0, 255), // Blue
                    ),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Palette>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual nullable field`() {
        @Serializable data class OptionalUUID(val name: String, @Contextual val id: UUID?)

        val module = SerializersModule { contextual(UUIDSerializer) }

        val ktoon = Ktoon(serializersModule = module)

        // Test with value
        val withValue = OptionalUUID("Test", UUID.randomUUID())
        val encodedWithValue = ktoon.encodeToString(withValue)
        val decodedWithValue = ktoon.decodeFromString<OptionalUUID>(encodedWithValue)
        assertEquals(withValue, decodedWithValue)

        // Test with null
        val withNull = OptionalUUID("Test", null)
        val encodedWithNull = ktoon.encodeToString(withNull)
        val decodedWithNull = ktoon.decodeFromString<OptionalUUID>(encodedWithNull)
        assertEquals(withNull, decodedWithNull)
    }

    @Test
    fun `multiple contextual types in module`() {
        @Serializable
        data class Document(
            @Contextual val id: UUID,
            val title: String,
            @Contextual val backgroundColor: Color,
        )

        val module = SerializersModule {
            contextual(UUIDSerializer)
            contextual(ColorSerializer)
        }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Document(
                id = UUID.randomUUID(),
                title = "My Document",
                backgroundColor = Color(255, 255, 255),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Document>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    @Ignore
    fun `contextual nested in polymorphic`() {
        @Serializable data class Page(@Contextual val id: UUID, val content: Content)

        val module = SerializersModule {
            contextual(UUIDSerializer)
            contextual(ColorSerializer)
        }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Page(
                id = UUID.randomUUID(),
                content = Content.Image("https://example.com/img.jpg", Color(240, 240, 240)),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Page>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `contextual serialization configuration`() {
        @Serializable
        data class Settings(
            @Contextual val sessionId: UUID,
            @Contextual val theme: Color,
            val username: String,
        )

        val module = SerializersModule {
            contextual(UUIDSerializer)
            contextual(ColorSerializer)
        }

        // Test with different configurations
        val original =
            Settings(sessionId = UUID.randomUUID(), theme = Color(0, 0, 0), username = "admin")

        // Default config
        val ktoonDefault = Ktoon(serializersModule = module)
        val encodedDefault = ktoonDefault.encodeToString(original)
        val decodedDefault = ktoonDefault.decodeFromString<Settings>(encodedDefault)
        assertEquals(original, decodedDefault)

        // Lenient config
        val ktoonLenient =
            Ktoon(serializersModule = module) {
                strictMode = false
                indentSize = 4
            }
        val encodedLenient = ktoonLenient.encodeToString(original)
        val decodedLenient = ktoonLenient.decodeFromString<Settings>(encodedLenient)
        assertEquals(original, decodedLenient)
    }
}
