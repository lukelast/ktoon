package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/** Captures field values or array elements during encoding. */
@OptIn(ExperimentalSerializationApi::class)
internal class ElementCapturer(
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val onComplete: (List<Pair<String, EncodedElement>>) -> Unit,
) : AbstractEncoder() {

    private val values = mutableListOf<Pair<String, EncodedElement>>()
    private var currentIndex = -1
    private var isArray = descriptor.kind == StructureKind.LIST

    private fun add(value: EncodedElement) {
        val name =
            if (isArray) {
                currentIndex.toString()
            } else {
                descriptor.getElementName(currentIndex)
            }
        values.add(name to value)
    }

    private fun quote(value: String) =
        StringQuoting.quote(
            value,
            StringQuoting.QuotingContext.ARRAY_ELEMENT,
            config.delimiter.char,
        )

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentIndex = index
        return true
    }

    override fun encodeNull() {
        add(EncodedElement.Primitive("null"))
    }

    override fun encodeBoolean(value: Boolean) {
        add(EncodedElement.Primitive(if (value) "true" else "false"))
    }

    override fun encodeByte(value: Byte) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeShort(value: Short) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeInt(value: Int) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeLong(value: Long) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeFloat(value: Float) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeDouble(value: Double) {
        add(EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeChar(value: Char) {
        add(EncodedElement.Primitive(quote(value.toString())))
    }

    override fun encodeString(value: String) {
        add(EncodedElement.Primitive(quote(value)))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        add(EncodedElement.Primitive(quote(enumDescriptor.getElementName(index))))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when (descriptor.kind) {
            StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            StructureKind.CLASS,
            StructureKind.OBJECT ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(EncodedElement.Structure(descriptor, it))
                }
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) {
        onComplete(values)
    }
}
