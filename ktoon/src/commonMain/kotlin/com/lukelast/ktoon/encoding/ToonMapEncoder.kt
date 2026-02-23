package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/** Encoder for TOON maps (key-value pairs). */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonMapEncoder(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val isRoot: Boolean = false,
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder() {

    private var currentKey: String? = null
    private var isKey = true

    /**
     * Maps always encode all entries — unlike class properties, map entries don't have "default
     * values" in the serialization sense. An entry either exists or it doesn't. The
     * [KtoonConfiguration.encodeDefaults] config applies to class properties with declared defaults.
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
            currentKey = value
        } else {
            val key = currentKey ?: error("Map value encoded without preceding key")
            writer.writeKeyValue(quoteKey(key), value)
            currentKey = null
        }
    }

    override fun encodeNull() = encodePrimitive("null")

    override fun encodeBoolean(value: Boolean) = encodePrimitive(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = encodePrimitive(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) = encodePrimitive(NumberNormalizer.normalize(value))

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

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (isKey) {
            throw IllegalArgumentException("TOON does not support complex keys in maps")
        } else {
            val key = currentKey ?: error("Map value structure started without preceding key")
            currentKey = null
            return delegateStructure(descriptor, key)
        }
    }

    private fun delegateStructure(descriptor: SerialDescriptor, key: String): CompositeEncoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                ToonArrayEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = indentLevel,
                    key = key,
                )
            }
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                writer.writeKey(quoteKey(key))
                ToonObjectEncoder(
                    rawWriter = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = indentLevel + 1,
                    isRoot = false,
                )
            }
            StructureKind.MAP -> {
                writer.writeKey(quoteKey(key))
                ToonMapEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = indentLevel + 1,
                    isRoot = false,
                )
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
