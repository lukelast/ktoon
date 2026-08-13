package com.lukelast.ktoon.serializers

import com.lukelast.ktoon.KtoonDecodingException
import com.lukelast.ktoon.decoding.ToonValue
import com.lukelast.ktoon.decoding.ToonValueSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Serializer for [JsonElement] that bridges the JSON data model and the TOON format.
 *
 * Serialization works with any [Encoder], allowing a [JsonElement] to be encoded to TOON.
 * Deserialization is format-specific and only works with decoders created by
 * [com.lukelast.ktoon.Ktoon], mirroring how kotlinx's own JsonElement serializer requires a
 * JsonDecoder. Decoded numbers carry the host representation (Long or Double), so their textual
 * form is normalized (e.g. `1e21` becomes `1.0E21`) while the mathematical value is preserved.
 */
object KtoonJsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonElement")

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("ReturnCount")
    override fun serialize(encoder: Encoder, value: JsonElement) {
        when (value) {
            is JsonNull -> encoder.encodeNull()
            is JsonPrimitive -> {
                if (value.isString) {
                    encoder.encodeString(value.content)
                } else {
                    val boolean = value.booleanOrNull
                    if (boolean != null) {
                        encoder.encodeBoolean(boolean)
                        return
                    }

                    val long = value.longOrNull
                    if (long != null) {
                        encoder.encodeLong(long)
                        return
                    }

                    val double = value.doubleOrNull
                    if (double != null) {
                        encoder.encodeDouble(double)
                        return
                    }

                    // Fallback to string if it's not a recognized primitive type
                    encoder.encodeString(value.content)
                }
            }
            is JsonArray -> {
                ListSerializer(this).serialize(encoder, value)
            }
            is JsonObject -> {
                val descriptor =
                    buildClassSerialDescriptor("JsonObject") {
                        value.keys.forEach { k -> element(k, descriptor) }
                    }
                val composite = encoder.beginStructure(descriptor)
                var index = 0
                for ((_, v) in value) {
                    composite.encodeSerializableElement(descriptor, index++, this, v)
                }
                composite.endStructure(descriptor)
            }
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        val source =
            decoder as? ToonValueSource
                ?: throw KtoonDecodingException(
                    "KtoonJsonElementSerializer requires a Ktoon decoder, " +
                        "got ${decoder::class.simpleName}"
                )
        return source.currentToonValue().toJsonElement()
    }

    private fun ToonValue.toJsonElement(): JsonElement =
        when (this) {
            is ToonValue.Null -> JsonNull
            is ToonValue.Boolean -> JsonPrimitive(value)
            is ToonValue.Number -> JsonPrimitive(value)
            is ToonValue.String -> JsonPrimitive(value)
            is ToonValue.Object -> JsonObject(properties.mapValues { (_, v) -> v.toJsonElement() })
            is ToonValue.Array -> JsonArray(elements.map { it.toJsonElement() })
        }
}
