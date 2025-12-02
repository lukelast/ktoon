package com.lukelast.ktoon.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Serializer for [JsonElement] that works with any [Encoder], not just JsonEncoder.
 *
 * This allows encoding a [JsonElement] (parsed from JSON) into other formats like TOON.
 * Note: This serializer currently only supports encoding (serialization).
 */
object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonElement")

    @OptIn(ExperimentalSerializationApi::class)
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
                val descriptor = buildClassSerialDescriptor("JsonObject") {
                    value.keys.forEach { k ->
                        element(k, JsonElementSerializer.descriptor)
                    }
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
        TODO("Deserialization is not yet supported for generic JsonElementSerializer")
    }
}
