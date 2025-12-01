package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/** Encoder for TOON arrays in inline, tabular, or expanded format. */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonArrayEncoder(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val key: String?,
) : AbstractEncoder() {

    private val elements = ArrayList<EncodedElement>(64)
    private var elementDescriptor: SerialDescriptor? = null

    sealed class EncodedElement {
        class Primitive(val value: String) : EncodedElement()

        data class Structure(
            val descriptor: SerialDescriptor,
            val values: List<Pair<String, EncodedElement>>,
        ) : EncodedElement() {
            val fieldNames = values.map(Pair<String, EncodedElement>::first)
        }

        data class NestedArray(val elements: List<EncodedElement>) : EncodedElement()
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (elementDescriptor == null && descriptor.elementsCount > 0)
            elementDescriptor = descriptor.getElementDescriptor(0)
        return true
    }

    private fun addPrimitive(value: String) {
        elements.add(EncodedElement.Primitive(value))
    }

    private fun quote(value: String) =
        StringQuoting.quote(
            value,
            StringQuoting.QuotingContext.ARRAY_ELEMENT,
            config.delimiter.char,
        )

    private fun quoteKey(value: String) =
        StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)

    override fun encodeNull() {
        addPrimitive("null")
    }

    override fun encodeBoolean(value: Boolean) {
        addPrimitive(if (value) "true" else "false")
    }

    override fun encodeByte(value: Byte) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeShort(value: Short) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeInt(value: Int) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeLong(value: Long) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeFloat(value: Float) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeDouble(value: Double) {
        addPrimitive(NumberNormalizer.normalize(value))
    }

    override fun encodeChar(value: Char) {
        addPrimitive(quote(value.toString()))
    }

    override fun encodeString(value: String) {
        addPrimitive(quote(value))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        addPrimitive(quote(enumDescriptor.getElementName(index)))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (elementDescriptor == null) elementDescriptor = descriptor
        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT ->
                ElementCapturer(config, serializersModule, descriptor) {
                    elements.add(EncodedElement.Structure(descriptor, it))
                }
            StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    elements.add(EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val format = ArrayFormatSelector.selectFormat(elements)
        when (format) {
            ArrayFormatSelector.ArrayFormat.INLINE -> writeInline()
            ArrayFormatSelector.ArrayFormat.TABULAR -> writeTabular()
            ArrayFormatSelector.ArrayFormat.EXPANDED -> writeExpanded()
        }
    }

    private fun writeInline() {
        writeArrayHeader(key, elements.size, config.delimiter.char)
        if (elements.isNotEmpty()) writer.writeSpace()
        elements.forEachIndexed { i, e ->
            if (e is EncodedElement.Primitive) {
                writer.write(e.value)
                if (i < elements.lastIndex) writer.writeDelimiter()
            }
        }
    }

    private fun writeTabular() {
        if (elements.isEmpty()) {
            writeInline()
            return
        }
        writeTabularImpl(key, elements, indentLevel)
    }

    private fun writeExpanded() {
        writeArrayHeader(key, elements.size, config.delimiter.char)
        elements.forEach { writeElement(it, indentLevel + 1) }
    }

    private fun writeElement(element: EncodedElement, indent: Int) {
        writer.writeNewline()
        writer.writeIndent(indent)
        writer.writeDash()
        writer.writeSpace()
        when (element) {
            is EncodedElement.Primitive -> writer.write(element.value)
            is EncodedElement.Structure -> writeStructureFields(element.values, indent)
            is EncodedElement.NestedArray -> writeNestedArrayImpl(null, element.elements, indent)
        }
    }

    private fun writeStructureFields(
        values: List<Pair<String, EncodedElement>>,
        indent: Int,
        firstInline: Boolean = true,
    ) {
        if (values.isEmpty() && !firstInline) {
            writer.writeSpace()
            writer.write("{}")
            return
        }
        values.forEachIndexed { i, (name, value) ->
            if (i > 0 || !firstInline) {
                writer.writeNewline()
                writer.writeIndent(indent + 1)
            }
            val qk = quoteKey(name)
            when (value) {
                is EncodedElement.Primitive -> writer.writeKeyValue(qk, value.value)
                is EncodedElement.NestedArray ->
                    writeNestedArrayImpl(qk, value.elements, indent + 1)
                is EncodedElement.Structure -> {
                    writer.writeKey(qk)
                    writeStructureFields(value.values, indent + 1, false)
                }
            }
        }
    }

    private fun writeNestedArrayImpl(key: String?, elements: List<EncodedElement>, indent: Int) {
        val format = ArrayFormatSelector.selectFormat(elements)
        val delim = config.delimiter.char
        when (format) {
            ArrayFormatSelector.ArrayFormat.INLINE -> {
                writeArrayHeader(key, elements.size, delim)
                if (elements.isNotEmpty()) writer.writeSpace()
                elements.forEachIndexed { i, e ->
                    writer.write((e as EncodedElement.Primitive).value)
                    if (i < elements.lastIndex) writer.writeDelimiter()
                }
            }
            ArrayFormatSelector.ArrayFormat.TABULAR -> writeTabularImpl(key, elements, indent)
            ArrayFormatSelector.ArrayFormat.EXPANDED -> {
                writeArrayHeader(key, elements.size, delim)
                elements.forEach { writeElement(it, indent + 1) }
            }
        }
    }

    private fun writeArrayHeader(key: String?, size: Int, delim: Char) {
        if (key != null) {
            writer.writeArrayHeader(key, size, delim)
        } else {
            writer.write('[')
            writer.write(size)
            if (delim != ',') writer.write(delim)
            writer.write("]:")
        }
    }

    private fun writeTabularImpl(key: String?, elements: List<EncodedElement>, indent: Int) {
        val first = elements.first() as EncodedElement.Structure
        val fields = ArrayFormatSelector.getFieldNames(first.descriptor).map { quoteKey(it) }
        val delim = config.delimiter.char
        if (key != null) {
            writer.writeTabularArrayHeader(key, elements.size, fields, delim)
        } else {
            writer.write('[')
            writer.write(elements.size)
            if (delim != ',') writer.write(delim)
            writer.write("]{")
            fields.forEachIndexed { i, f ->
                writer.write(f)
                if (i < fields.lastIndex) writer.write(delim)
            }
            writer.write("}:")
        }
        elements.filterIsInstance<EncodedElement.Structure>().forEach { s ->
            writer.writeNewline()
            writer.writeIndent(indent + 1)
            s.values.forEachIndexed { i, (_, v) ->
                writer.write(if (v is EncodedElement.Primitive) v.value else v.toString())
                if (i < s.values.lastIndex) writer.writeDelimiter()
            }
        }
    }
}

/** Captures field values or array elements during encoding. */
@OptIn(ExperimentalSerializationApi::class)
private class ElementCapturer(
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val onComplete: (List<Pair<String, ToonArrayEncoder.EncodedElement>>) -> Unit,
) : AbstractEncoder() {

    private val values = mutableListOf<Pair<String, ToonArrayEncoder.EncodedElement>>()
    private var currentIndex = -1
    private var isArray = descriptor.kind == StructureKind.LIST

    private fun add(value: ToonArrayEncoder.EncodedElement) {
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
        add(ToonArrayEncoder.EncodedElement.Primitive("null"))
    }

    override fun encodeBoolean(value: Boolean) {
        add(ToonArrayEncoder.EncodedElement.Primitive(if (value) "true" else "false"))
    }

    override fun encodeByte(value: Byte) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeShort(value: Short) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeInt(value: Int) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeLong(value: Long) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeFloat(value: Float) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeDouble(value: Double) {
        add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value)))
    }

    override fun encodeChar(value: Char) {
        add(ToonArrayEncoder.EncodedElement.Primitive(quote(value.toString())))
    }

    override fun encodeString(value: String) {
        add(ToonArrayEncoder.EncodedElement.Primitive(quote(value)))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        add(ToonArrayEncoder.EncodedElement.Primitive(quote(enumDescriptor.getElementName(index))))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when (descriptor.kind) {
            StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(ToonArrayEncoder.EncodedElement.NestedArray(it.map { (_, v) -> v }))
                }
            StructureKind.CLASS,
            StructureKind.OBJECT ->
                ElementCapturer(config, serializersModule, descriptor) {
                    add(ToonArrayEncoder.EncodedElement.Structure(descriptor, it))
                }
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) {
        onComplete(values)
    }
}
