package com.lukelast.ktoon.serializers

import com.lukelast.ktoon.DEFAULT_MAX_NESTING_DEPTH
import com.lukelast.ktoon.KtoonDecodingException
import com.lukelast.ktoon.KtoonEncodingException
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
 * Maximum number of nested JSON containers this serializer will walk. Serialization recurses per
 * container, so an unbounded tree would exhaust the host stack; SPEC §15 lets implementations
 * document such a limit and report exceeding it as an error instead.
 */
internal const val MAX_JSON_NESTING_DEPTH: Int = DEFAULT_MAX_NESTING_DEPTH

/**
 * Serializer for [JsonElement] that bridges the JSON data model and the TOON format.
 *
 * Serialization works with any [Encoder], allowing a [JsonElement] to be encoded to TOON.
 * Deserialization is format-specific and only works with decoders created by
 * [com.lukelast.ktoon.Ktoon], mirroring how kotlinx's own JsonElement serializer requires a
 * JsonDecoder. Decoded numbers carry the host representation (Long or Double), so their textual
 * form is normalized (e.g. `1e21` becomes `1.0E21`) while the mathematical value is preserved.
 *
 * Values nested deeper than [MAX_JSON_NESTING_DEPTH] containers are rejected with a
 * [KtoonEncodingException].
 */
object KtoonJsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonElement")

    override fun serialize(encoder: Encoder, value: JsonElement) {
        JsonElementSerializerAtDepth(0).serialize(encoder, value)
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

/**
 * Encodes a [JsonElement] whose containers sit [depth] levels below the root. The depth is carried
 * immutably so that [KtoonJsonElementSerializer] stays a shared, stateless singleton.
 */
private class JsonElementSerializerAtDepth(private val depth: Int) : KSerializer<JsonElement> {

    override val descriptor: SerialDescriptor = KtoonJsonElementSerializer.descriptor

    override fun deserialize(decoder: Decoder): JsonElement =
        KtoonJsonElementSerializer.deserialize(decoder)

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
                ListSerializer(childSerializer()).serialize(encoder, value)
            }
            is JsonObject -> {
                val child = childSerializer()
                val descriptor =
                    buildClassSerialDescriptor("JsonObject") {
                        value.keys.forEach { k -> element(k, descriptor) }
                    }
                val composite = encoder.beginStructure(descriptor)
                var index = 0
                for ((_, v) in value) {
                    composite.encodeSerializableElement(descriptor, index++, child, v)
                }
                composite.endStructure(descriptor)
            }
        }
    }

    private fun childSerializer(): JsonElementSerializerAtDepth {
        if (depth >= MAX_JSON_NESTING_DEPTH) {
            throw KtoonEncodingException(
                "Maximum JSON nesting depth of $MAX_JSON_NESTING_DEPTH exceeded"
            )
        }
        return JsonElementSerializerAtDepth(depth + 1)
    }
}

/**
 * Rejects raw JSON text nested deeper than [MAX_JSON_NESTING_DEPTH] before it reaches the JSON
 * parser, whose own tree reader recurses per container. Only structural brackets outside string
 * literals are counted; all syntax validation is left to the parser.
 */
internal fun checkJsonNestingDepth(json: String) {
    var depth = 0
    var inString = false
    var escaped = false
    for (c in json) {
        when {
            escaped -> escaped = false
            inString && c == '\\' -> escaped = true
            c == '"' -> inString = !inString
            inString -> {}
            c == '[' || c == '{' -> {
                depth++
                if (depth > MAX_JSON_NESTING_DEPTH) {
                    throw KtoonEncodingException(
                        "Maximum JSON nesting depth of $MAX_JSON_NESTING_DEPTH exceeded"
                    )
                }
            }
            c == ']' || c == '}' -> depth--
        }
    }
}
