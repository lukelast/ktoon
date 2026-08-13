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

/** Captures field values or array elements during encoding. */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ElementCapturer(
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val onComplete: (List<Pair<String, EncodedElement>>) -> Unit,
) : AbstractEncoder(), ToonNumberSink {

    override fun encodeNumberLiteral(literal: String) =
        add(EncodedElement.Primitive(NumberNormalizer.normalizeLiteral(literal)))

    private val entries = mutableListOf<Pair<String, EncodedElement>>()
    private var currentIndex = -1
    private val isArray = descriptor.kind == StructureKind.LIST

    private fun add(value: EncodedElement) {
        val name =
            if (isArray) {
                currentIndex.toString()
            } else {
                descriptor.getElementName(currentIndex)
            }
        entries.add(name to value)
    }

    private fun quoteElement(value: String) =
        StringQuoting.quote(
            value,
            StringQuoting.QuotingContext.ARRAY_ELEMENT,
            config.delimiter.char,
        )

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) =
        config.encodeDefaults

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
        add(EncodedElement.Primitive(quoteElement(value.toString())))
    }

    override fun encodeString(value: String) {
        add(EncodedElement.Primitive(quoteElement(value)))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        add(EncodedElement.Primitive(quoteElement(enumDescriptor.getElementName(index))))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { add(EncodedElement.Primitive(it)) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when {
            descriptor.kind == StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            descriptor.isObjectKind() ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(EncodedElement.Structure(it))
                }
            descriptor.kind == StructureKind.MAP ->
                MapElementCapturer(config, serializersModule) { add(EncodedElement.Structure(it)) }
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) {
        // The sortFields option orders an object's declared fields alphabetically. Array elements
        // and map entries keep their encounter order, so only class/object captures are sorted.
        val sortable =
            config.sortFields &&
                (descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT)
        onComplete(if (sortable) entries.sortedBy { it.first } else entries)
    }
}

/**
 * Captures map entries during encoding. Keys are captured raw (unquoted) so the writer can apply
 * key quoting; values are captured like [ElementCapturer] elements.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class MapElementCapturer(
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val onComplete: (List<Pair<String, EncodedElement>>) -> Unit,
) : AbstractEncoder(), ToonNumberSink {

    override fun encodeNumberLiteral(literal: String) =
        addPrimitive(NumberNormalizer.normalizeLiteral(literal))

    private val entries = mutableListOf<Pair<String, EncodedElement>>()
    private var isKey = true
    private var currentKey: String? = null
    private val keyNames = MapKeyNames()

    /** Map entries don't have defaults in the serialization sense; always encode them. */
    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = true

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        isKey = (index % 2 == 0)
        return true
    }

    private fun quoteElement(value: String) =
        StringQuoting.quote(
            value,
            StringQuoting.QuotingContext.ARRAY_ELEMENT,
            config.delimiter.char,
        )

    private fun addPrimitive(raw: String, encoded: String = raw) {
        if (isKey) {
            currentKey = keyNames.claim(raw)
        } else {
            addValue(EncodedElement.Primitive(encoded))
        }
    }

    private fun addValue(value: EncodedElement) {
        val key = currentKey ?: error("Map value encoded without preceding key")
        entries.add(key to value)
        currentKey = null
    }

    override fun encodeNull() {
        // §2/§3: a TOON object maps string keys to values, so a null key has no representation.
        if (isKey) throw KtoonEncodingException("TOON does not support null keys in maps")
        addPrimitive("null")
    }

    override fun encodeBoolean(value: Boolean) = addPrimitive(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) =
        addPrimitive(if (isKey) keyNames.format(value) else NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) =
        addPrimitive(if (isKey) keyNames.format(value) else NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) =
        addPrimitive(value.toString(), quoteElement(value.toString()))

    override fun encodeString(value: String) = addPrimitive(value, quoteElement(value))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val name = enumDescriptor.getElementName(index)
        addPrimitive(name, quoteElement(name))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { addPrimitive(it) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        require(!isKey) { "TOON does not support complex keys in maps" }
        return when {
            descriptor.kind == StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    addValue(EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            descriptor.isObjectKind() ->
                ElementCapturer(config, serializersModule, descriptor) {
                    addValue(EncodedElement.Structure(it))
                }
            descriptor.kind == StructureKind.MAP ->
                MapElementCapturer(config, serializersModule) {
                    addValue(EncodedElement.Structure(it))
                }
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        onComplete(entries)
    }
}
