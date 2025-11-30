package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/** Encoder for TOON objects (structures with named fields). */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonObjectEncoder(
    private val writer: ToonWriter,
    private val config: ToonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val indentLevel: Int,
    private val isRoot: Boolean = false,
) : AbstractEncoder() {

    private var elementIndex = 0
    private var currentKey: String? = null

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elementIndex = index
        currentKey = descriptor.getElementName(index)
        if (!isRoot || elementIndex > 0) writer.writeNewline()
        writer.writeIndent(indentLevel)
        return true
    }

    override fun encodeNull() = writeKeyAndValue("null")
    override fun encodeBoolean(value: Boolean) = writeKeyAndValue(if (value) "true" else "false")
    override fun encodeByte(value: Byte) = writeKeyAndValue(NumberNormalizer.normalize(value))
    override fun encodeShort(value: Short) = writeKeyAndValue(NumberNormalizer.normalize(value))
    override fun encodeInt(value: Int) = writeKeyAndValue(NumberNormalizer.normalize(value))
    override fun encodeLong(value: Long) = writeKeyAndValue(NumberNormalizer.normalize(value))
    override fun encodeFloat(value: Float) = writeKeyAndValue(NumberNormalizer.normalize(value))
    override fun encodeDouble(value: Double) = writeKeyAndValue(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) = writeKeyAndValue(quoteValue(value.toString()))
    override fun encodeString(value: String) = writeKeyAndValue(quoteValue(value))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        writeKeyAndValue(quoteValue(enumDescriptor.getElementName(index)))

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val key = currentKey ?: "value"
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP -> {
                writeKey(key)
                ToonObjectEncoder(writer, config, serializersModule, descriptor, indentLevel + 1, false)
            }
            StructureKind.LIST ->
                ToonArrayEncoder(writer, config, serializersModule, descriptor, indentLevel, false, key)
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) { }

    private fun quoteValue(value: String) =
        StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_VALUE, config.delimiter.char)

    private fun quoteKey(key: String) =
        StringQuoting.quote(key, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)

    private fun writeKeyAndValue(value: String) {
        currentKey?.let { writer.writeKeyValue(quoteKey(it), value) }
    }

    private fun writeKey(key: String) = writer.writeKey(quoteKey(key))

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?,
    ) {
        if (value == null) {
            encodeElement(descriptor, index)
            encodeNull()
        } else {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }
}
