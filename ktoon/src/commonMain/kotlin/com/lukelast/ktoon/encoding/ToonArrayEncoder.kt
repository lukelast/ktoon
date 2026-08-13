package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.util.isObjectKind
import com.lukelast.ktoon.util.isUnsignedDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Encoder for TOON arrays. Elements are captured first; the mandated form (inline, tabular with
 * nested field groups, or list) is selected and written by [ElementWriter] once all elements are
 * known.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ToonArrayEncoder(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val key: String?,
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder(), ToonNumberSink {

    override fun encodeNumberLiteral(literal: String) =
        addPrimitive(NumberNormalizer.normalizeLiteral(literal))

    private val elements = ArrayList<EncodedElement>(64)

    private fun addPrimitive(value: String) {
        elements.add(EncodedElement.Primitive(value))
    }

    private fun quoteElement(value: String) =
        StringQuoting.quote(
            value,
            StringQuoting.QuotingContext.ARRAY_ELEMENT,
            config.delimiter.char,
        )

    override fun encodeNull() = addPrimitive("null")

    override fun encodeBoolean(value: Boolean) = addPrimitive(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) = addPrimitive(quoteElement(value.toString()))

    override fun encodeString(value: String) = addPrimitive(quoteElement(value))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        addPrimitive(quoteElement(enumDescriptor.getElementName(index)))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { addPrimitive(it) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when {
            descriptor.isObjectKind() ->
                ElementCapturer(config, serializersModule, descriptor) {
                    elements.add(EncodedElement.Structure(it))
                }
            descriptor.kind == StructureKind.MAP ->
                MapElementCapturer(config, serializersModule) {
                    elements.add(EncodedElement.Structure(it))
                }
            descriptor.kind == StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    elements.add(EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) {
        val position =
            if (key == null) ElementWriter.ArrayPosition.ROOT else ElementWriter.ArrayPosition.FIELD
        ElementWriter(writer, config).writeArray(key, elements, indentLevel, position)
        onEnd?.invoke()
    }
}
