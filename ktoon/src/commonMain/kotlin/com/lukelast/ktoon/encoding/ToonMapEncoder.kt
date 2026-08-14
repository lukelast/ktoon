package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonEncodingException
import com.lukelast.ktoon.util.isObjectKind
import com.lukelast.ktoon.util.isUnsignedDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/** Encoder for TOON maps (key-value pairs). */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions", "LongParameterList")
internal class ToonMapEncoder(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val depth: Int,
    private val isRoot: Boolean = false,
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder(), ToonNumberSink {

    init {
        config.checkEncoderNesting(depth)
    }

    override fun encodeNumberLiteral(literal: String) =
        encodePrimitive(NumberNormalizer.normalizeLiteral(literal))

    private var currentKey: String? = null
    private var isKey = true
    private val keyNames = MapKeyNames()

    /**
     * Maps always encode all entries — unlike class properties, map entries don't have "default
     * values" in the serialization sense. An entry either exists or it doesn't. The
     * [KtoonConfiguration.encodeDefaults] config applies to class properties with declared
     * defaults.
     */
    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = true

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        isKey = (index % 2 == 0)
        if (isKey) {
            if (!isRoot || index > 0) writer.writeNewline()
            writer.writeIndent(indentLevel)
        }
        return true
    }

    private fun encodePrimitive(value: String) {
        if (isKey) {
            currentKey = keyNames.claim(value)
        } else {
            val key = currentKey ?: error("Map value encoded without preceding key")
            writer.writeKeyValue(quoteKey(key), value)
            currentKey = null
        }
    }

    override fun encodeNull() {
        // §2/§3: a TOON object maps string keys to values, so a null key has no representation —
        // writing it as the text "null" would collide with the ordinary string key of that name.
        if (isKey) throw KtoonEncodingException("TOON does not support null keys in maps")
        encodePrimitive("null")
    }

    override fun encodeBoolean(value: Boolean) = encodePrimitive(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) =
        encodePrimitive(if (isKey) keyNames.format(value) else NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) =
        encodePrimitive(if (isKey) keyNames.format(value) else NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) {
        if (isKey) encodePrimitive(value.toString())
        else encodePrimitive(quoteValue(value.toString()))
    }

    override fun encodeString(value: String) {
        if (isKey) encodePrimitive(value) else encodePrimitive(quoteValue(value))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val name = enumDescriptor.getElementName(index)
        if (isKey) encodePrimitive(name) else encodePrimitive(quoteValue(name))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { encodePrimitive(it) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        require(!isKey) { "TOON does not support complex keys in maps" }
        val key = currentKey ?: error("Map value structure started without preceding key")
        currentKey = null
        return delegateStructure(descriptor, key)
    }

    private fun delegateStructure(descriptor: SerialDescriptor, key: String): CompositeEncoder {
        return when {
            descriptor.kind == StructureKind.LIST -> {
                // §9: capture first so the array's form follows from the elements themselves
                ElementCapturer.forArray(config, serializersModule, descriptor, depth + 1) {
                    elements ->
                    ElementWriter(writer, config)
                        .writeArray(key, elements, indentLevel, ElementWriter.ArrayPosition.FIELD)
                }
            }
            descriptor.isObjectKind() -> {
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    ElementCapturer.forObject(config, serializersModule, descriptor, depth + 1) {
                        entries ->
                        ElementWriter(writer, config).writeObjectField(key, entries, indentLevel)
                    }
                } else {
                    writer.writeKey(quoteKey(key))
                    ToonObjectEncoder(
                        rawWriter = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = indentLevel + 1,
                        depth = depth + 1,
                        isRoot = false,
                    )
                }
            }
            descriptor.kind == StructureKind.MAP -> {
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    MapElementCapturer(config, serializersModule, depth + 1) { entries ->
                        ElementWriter(writer, config).writeObjectField(key, entries, indentLevel)
                    }
                } else {
                    writer.writeKey(quoteKey(key))
                    ToonMapEncoder(
                        writer = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = indentLevel + 1,
                        depth = depth + 1,
                        isRoot = false,
                    )
                }
            }
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        onEnd?.invoke()
    }

    private fun quoteValue(value: String) =
        StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_VALUE, config.delimiter.char)

    private fun quoteKey(key: String) =
        StringQuoting.quote(key, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)
}
