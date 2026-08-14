package com.lukelast.ktoon.serializers

import com.lukelast.ktoon.KtoonDecodingException
import com.lukelast.ktoon.KtoonEncodingException
import com.lukelast.ktoon.decoding.ToonValue
import com.lukelast.ktoon.decoding.ToonValueSource
import com.lukelast.ktoon.decoding.matchesNumberGrammar
import com.lukelast.ktoon.encoding.ToonNumberSink
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
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Serializer for [JsonElement] that bridges the JSON data model and the TOON format.
 *
 * Serialization works with any [Encoder], allowing a [JsonElement] to be encoded to TOON.
 * Deserialization is format-specific and only works with decoders created by
 * [com.lukelast.ktoon.Ktoon], mirroring how kotlinx's own JsonElement serializer requires a
 * JsonDecoder. Decoded numbers keep the document's own literal, so their exact value survives even
 * when it needs more precision than a host `Long` or `Double` carries.
 *
 * Serialization recurses per container; the Ktoon encoders bound that recursion at
 * `maxNestingDepth` (SPEC §15), so trees nested deeper are rejected with a
 * [KtoonEncodingException].
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

                    // A JsonPrimitive holds its number as text. Converting it to a host Long or
                    // Double first would round a large integer and turn an out-of-range literal
                    // into null, so a TOON encoder is handed the digits instead.
                    val content = value.content
                    if (encoder is ToonNumberSink && matchesNumberGrammar(content)) {
                        encoder.encodeNumberLiteral(content)
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
            // §4: the accepted token carries the exact value, which the host Int/Long/Double may
            // only approximate, so schema-less JSON keeps the literal rather than the host form.
            is ToonValue.Number -> JsonUnquotedLiteral(lexeme)
            is ToonValue.String -> JsonPrimitive(value)
            is ToonValue.Object -> JsonObject(properties.mapValues { (_, v) -> v.toJsonElement() })
            is ToonValue.Array -> JsonArray(elements.map { it.toJsonElement() })
        }
}

/**
 * Rejects raw JSON text nested deeper than [maxDepth] containers before it reaches the JSON parser,
 * whose own tree reader recurses per container. Only structural brackets outside string literals
 * are counted; all syntax validation is left to the parser.
 */
internal fun checkJsonNestingDepth(json: String, maxDepth: Int) {
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
                if (depth > maxDepth) {
                    throw KtoonEncodingException("Maximum nesting depth of $maxDepth exceeded")
                }
            }
            c == ']' || c == '}' -> depth--
        }
    }
}
