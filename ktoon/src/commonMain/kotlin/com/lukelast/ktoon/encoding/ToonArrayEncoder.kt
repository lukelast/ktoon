package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
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
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder() {

    private val elements = ArrayList<EncodedElement>(64)
    private var elementDescriptor: SerialDescriptor? = null

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

    override fun encodeNull() = addPrimitive("null")

    override fun encodeBoolean(value: Boolean) = addPrimitive(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) = addPrimitive(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) = addPrimitive(quote(value.toString()))

    override fun encodeString(value: String) = addPrimitive(quote(value))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        addPrimitive(quote(enumDescriptor.getElementName(index)))

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
        onEnd?.invoke()
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
            ArrayFormatSelector.ArrayFormat.TABULAR -> {
                if (key != null) {
                    writeTabularImpl(key, elements, indent)
                } else {
                    writeArrayHeader(key, elements.size, delim)
                    elements.forEach { writeElement(it, indent + 1) }
                }
            }
            ArrayFormatSelector.ArrayFormat.EXPANDED -> {
                writeArrayHeader(key, elements.size, delim)
                elements.forEach { writeElement(it, indent + 1) }
            }
        }
    }

    private fun writeArrayHeader(key: String?, size: Int, delim: Char) {
        if (key != null) {
            writer.writeArrayHeader(quoteKey(key), size, delim)
        } else {
            writer.write('[')
            writer.write(size)
            if (delim != ',') writer.write(delim)
            writer.write("]:")
        }
    }

    private fun writeTabularImpl(key: String?, elements: List<EncodedElement>, indent: Int) {
        val first = elements.first() as EncodedElement.Structure
        val fields = first.descriptor.elementNames.map(::quoteKey)
        val delim = config.delimiter.char
        if (key != null) {
            writer.writeTabularArrayHeader(quoteKey(key), elements.size, fields, delim)
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
            val valueMap = s.values.toMap()
            // We use 'first.fieldNames' (not 'fields' which are quoted) to look up values
            first.values.forEachIndexed { i, (name, _) ->
                val v = valueMap[name]
                // If v is null, it means the field is missing, which shouldn't happen if sets
                // match.
                // But if it does, we might want to write null or error.
                // Given ArrayFormatSelector ensures sets match, v should be present.
                if (v != null) {
                    writer.write(if (v is EncodedElement.Primitive) v.value else v.toString())
                } else {
                    // Should not happen if ArrayFormatSelector is correct
                    writer.write("null")
                }
                if (i < first.values.lastIndex) writer.writeDelimiter()
            }
        }
    }
}
