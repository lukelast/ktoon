package com.lukelast.ktoon

import com.lukelast.ktoon.decoding.ToonDecoder
import com.lukelast.ktoon.decoding.ToonLexer
import com.lukelast.ktoon.decoding.ToonReader
import com.lukelast.ktoon.encoding.ToonEncoder
import com.lukelast.ktoon.encoding.ToonWriter
import com.lukelast.ktoon.serializers.JsonElementSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Main entry point for TOON (Token-Oriented Object Notation) format serialization.
 *
 * TOON is a line-oriented, indentation-based text format that encodes the JSON data model with
 * explicit structure and minimal quoting. It's particularly efficient for arrays of uniform objects
 * and provides a compact, deterministic representation of structured data.
 *
 * Example usage:
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val ktoon = Ktoon()
 * val user = User(1, "Alice")
 *
 * // Encoding
 * val encoded = ktoon.encodeToString(user)
 * // Output:
 * // id: 1
 * // name: Alice
 *
 * // Decoding
 * val decoded = ktoon.decodeFromString<User>(encoded)
 * ```
 *
 * @property serializersModule Module with contextual and polymorphic serializers
 * @property configuration Configuration for TOON format behavior
 */
class Ktoon(
    val serializersModule: SerializersModule = EmptySerializersModule(),
    val configuration: KtoonConfiguration = KtoonConfiguration.Default,
) {

    /**
     * Encodes the given [value] to a TOON format string using the given [serializer].
     *
     * @param serializer The serialization strategy for type T
     * @param value The value to encode
     * @return The encoded TOON string
     * @throws KtoonEncodingException if encoding fails
     */
    fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val writer = ToonWriter(configuration)
        val encoder = ToonEncoder(writer, configuration, serializersModule)

        try {
            encoder.encodeSerializableValue(serializer, value)
            return writer.toString()
        } catch (e: KtoonException) {
            throw e
        } catch (e: Exception) {
            throw KtoonEncodingException("Failed to encode value to TOON format", e)
        }
    }

    /**
     * Decodes a TOON format string to a value of type T using the given [deserializer].
     *
     * @param deserializer The deserialization strategy for type T
     * @param string The TOON format string to decode
     * @return The decoded value
     * @throws KtoonDecodingException if decoding fails
     * @throws KtoonParsingException if parsing fails
     * @throws KtoonValidationException if validation fails (in strict mode)
     */
    fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        try {
            // Tokenize the input
            val lexer = ToonLexer(string, configuration)
            val tokens = lexer.tokenize()

            // Parse tokens into structured data
            val reader = ToonReader(tokens, configuration)

            // Decode using kotlinx.serialization
            val decoder = ToonDecoder(reader, serializersModule, configuration)
            return decoder.decodeSerializableValue(deserializer)
        } catch (e: KtoonException) {
            throw e
        } catch (e: Exception) {
            throw KtoonDecodingException("Failed to decode TOON format string", e)
        }
    }

    /**
     * Convenience method to encode a value with reified type parameter.
     *
     * Example:
     * ```kotlin
     * val ktoon = Ktoon()
     * val user = User(1, "Alice")
     * val encoded = ktoon.encodeToString(user)
     * ```
     */
    inline fun <reified T> encodeToString(value: T): String =
        encodeToString(kotlinx.serialization.serializer(), value)

    fun encodeJsonToToon(jsonElement: JsonElement): String =
        encodeToString(JsonElementSerializer, jsonElement)

    fun encodeJsonToToon(json: String): String {
        val jsonElement = Json.parseToJsonElement(json)
        return encodeJsonToToon(jsonElement)
    }

    /**
     * Convenience method to decode a value with reified type parameter.
     *
     * Example:
     * ```kotlin
     * val ktoon = Ktoon()
     * val user = ktoon.decodeFromString<User>(encoded)
     * ```
     */
    inline fun <reified T> decodeFromString(string: String): T =
        decodeFromString(kotlinx.serialization.serializer(), string)

    companion object {
        /** Default TOON instance with strict mode enabled. */
        val Default = Ktoon()

        /**
         * Compact TOON instance optimized for minimal output size. Enables key folding for more
         * compact representation.
         */
        val Compact = Ktoon(configuration = KtoonConfiguration.Compact)

        /**
         * Creates a Ktoon instance with a custom configuration using a DSL-style builder.
         *
         * Example:
         * ```kotlin
         * val ktoon = Ktoon {
         *     strictMode = false
         *     keyFolding = true
         *     indentSize = 4
         * }
         * ```
         */
        operator fun invoke(builder: KtoonConfigurationBuilder.() -> Unit): Ktoon {
            val config = KtoonConfigurationBuilder().apply(builder).build()
            return Ktoon(configuration = config)
        }

        /**
         * Creates a Ktoon instance with a custom configuration and serializers module.
         *
         * Example:
         * ```kotlin
         * val ktoon = Ktoon(serializersModule) {
         *     strictMode = true
         *     keyFolding = false
         * }
         * ```
         */
        operator fun invoke(
            serializersModule: SerializersModule,
            builder: KtoonConfigurationBuilder.() -> Unit,
        ): Ktoon {
            val config = KtoonConfigurationBuilder().apply(builder).build()
            return Ktoon(serializersModule, config)
        }
    }
}
