package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration
import com.lukelast.ktoon.ToonEncodingException
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
    private val config: ToonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val indentLevel: Int,
    private val isRoot: Boolean,
    private val key: String?,
) : AbstractEncoder() {

    private val elements = mutableListOf<EncodedElement>()
    private var elementDescriptor: SerialDescriptor? = null

    sealed class EncodedElement {
        data class Primitive(val value: String) : EncodedElement()
        data class Structure(val descriptor: SerialDescriptor, val values: List<Pair<String, EncodedElement>>) : EncodedElement()
        data class NestedArray(val elements: List<EncodedElement>) : EncodedElement()
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (elementDescriptor == null && descriptor.elementsCount > 0)
            elementDescriptor = descriptor.getElementDescriptor(0)
        return true
    }

    private fun addPrimitive(value: String) { elements.add(EncodedElement.Primitive(value)) }
    private fun quote(value: String) = StringQuoting.quote(value, StringQuoting.QuotingContext.ARRAY_ELEMENT, config.delimiter.char)
    private fun quoteKey(value: String) = StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)

    override fun encodeNull() { addPrimitive("null") }
    override fun encodeBoolean(value: Boolean) { addPrimitive(if (value) "true" else "false") }
    override fun encodeByte(value: Byte) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeShort(value: Short) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeInt(value: Int) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeLong(value: Long) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeFloat(value: Float) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeDouble(value: Double) { addPrimitive(NumberNormalizer.normalize(value)) }
    override fun encodeChar(value: Char) { addPrimitive(quote(value.toString())) }
    override fun encodeString(value: String) { addPrimitive(quote(value)) }
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) { addPrimitive(quote(enumDescriptor.getElementName(index))) }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (elementDescriptor == null) elementDescriptor = descriptor
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT ->
                ElementCapturer(config, serializersModule, descriptor) { elements.add(EncodedElement.Structure(descriptor, it)) }
            StructureKind.LIST ->
                ElementCapturer(config, serializersModule, descriptor) { elements.add(EncodedElement.NestedArray(it.map { (_, v) -> v })) }
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val format = selectFormat()
        when (format) {
            ArrayFormatSelector.ArrayFormat.INLINE -> writeInline()
            ArrayFormatSelector.ArrayFormat.TABULAR -> writeTabular()
            ArrayFormatSelector.ArrayFormat.EXPANDED -> writeExpanded()
        }
    }

    private fun selectFormat(): ArrayFormatSelector.ArrayFormat {
        if (elements.all { it is EncodedElement.Primitive }) return ArrayFormatSelector.ArrayFormat.INLINE
        if (elements.all { it is EncodedElement.Structure }) {
            val structures = elements.filterIsInstance<EncodedElement.Structure>()
            if (structures.isNotEmpty()) {
                val first = structures.first()
                val allSame = structures.all { it.descriptor == first.descriptor }
                        && ArrayFormatSelector.allPropertiesArePrimitive(first.descriptor)
                        && structures.all { it.values.map { p -> p.first }.toSet() == first.values.map { p -> p.first }.toSet() }
                if (allSame) return ArrayFormatSelector.ArrayFormat.TABULAR
            }
        }
        return ArrayFormatSelector.ArrayFormat.EXPANDED
    }

    private fun writeHeader(key: String?) {
        if (key != null) writer.writeArrayHeader(key, elements.size, config.delimiter.char)
        else {
            writer.write("[${elements.size}")
            if (config.delimiter.char != ',') writer.write(config.delimiter.char.toString())
            writer.write("]:")
        }
    }

    private fun writeInline() {
        writeHeader(key)
        if (elements.isNotEmpty()) writer.writeSpace()
        elements.forEachIndexed { i, e ->
            if (e is EncodedElement.Primitive) {
                writer.write(e.value)
                if (i < elements.lastIndex) writer.writeDelimiter()
            }
        }
    }

    private fun writeTabular() {
        if (elements.isEmpty()) { writeInline(); return }
        val first = elements.first() as? EncodedElement.Structure
            ?: throw ToonEncodingException.unsupportedType("Non-structure in tabular array")
        val fields = ArrayFormatSelector.getFieldNames(first.descriptor).map { quoteKey(it) }
        if (key != null) writer.writeTabularArrayHeader(key, elements.size, fields, config.delimiter.char)
        else {
            writer.write("[${elements.size}")
            if (config.delimiter.char != ',') writer.write(config.delimiter.char.toString())
            writer.write("]{${fields.joinToString(config.delimiter.char.toString())}}:")
        }
        elements.filterIsInstance<EncodedElement.Structure>().forEach { s ->
            writer.writeNewline()
            writer.writeIndent(indentLevel + 1)
            s.values.forEachIndexed { i, (_, v) ->
                writer.write(if (v is EncodedElement.Primitive) v.value else v.toString())
                if (i < s.values.lastIndex) writer.writeDelimiter()
            }
        }
    }

    private fun writeExpanded() {
        writeHeader(key)
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
            is EncodedElement.NestedArray -> writeNestedArray(element.elements, indent)
        }
    }

    private fun writeStructureFields(values: List<Pair<String, EncodedElement>>, indent: Int) {
        values.forEachIndexed { i, (name, value) ->
            if (i > 0) { writer.writeNewline(); writer.writeIndent(indent + 1) }
            val qk = quoteKey(name)
            when (value) {
                is EncodedElement.Primitive -> writer.writeKeyValue(qk, value.value)
                is EncodedElement.NestedArray -> writeNestedArrayField(qk, value.elements, indent + 1)
                is EncodedElement.Structure -> { writer.writeKey(qk); writeNestedStructure(value.values, indent + 1) }
            }
        }
    }

    private fun writeNestedStructure(values: List<Pair<String, EncodedElement>>, indent: Int) {
        if (values.isEmpty()) { writer.writeSpace(); writer.write("{}"); return }
        values.forEach { (name, value) ->
            writer.writeNewline()
            writer.writeIndent(indent + 1)
            val qk = quoteKey(name)
            when (value) {
                is EncodedElement.Primitive -> writer.writeKeyValue(qk, value.value)
                is EncodedElement.NestedArray -> writeNestedArrayField(qk, value.elements, indent + 1)
                is EncodedElement.Structure -> { writer.writeKey(qk); writeNestedStructure(value.values, indent + 1) }
            }
        }
    }

    private fun writeNestedArray(elements: List<EncodedElement>, indent: Int) {
        val format = selectNestedFormat(elements)
        when (format) {
            ArrayFormatSelector.ArrayFormat.INLINE -> {
                writer.write("[${elements.size}")
                if (config.delimiter.char != ',') writer.write(config.delimiter.char.toString())
                writer.write("]:")
                if (elements.isNotEmpty()) writer.writeSpace()
                elements.forEachIndexed { i, e ->
                    writer.write((e as EncodedElement.Primitive).value)
                    if (i < elements.lastIndex) writer.writeDelimiter()
                }
            }
            ArrayFormatSelector.ArrayFormat.TABULAR -> {
                val first = elements.first() as EncodedElement.Structure
                val fields = ArrayFormatSelector.getFieldNames(first.descriptor).map { quoteKey(it) }
                writer.write("[${elements.size}")
                if (config.delimiter.char != ',') writer.write(config.delimiter.char.toString())
                writer.write("]{${fields.joinToString(config.delimiter.char.toString())}}:")
                elements.filterIsInstance<EncodedElement.Structure>().forEach { s ->
                    writer.writeNewline()
                    writer.writeIndent(indent + 1)
                    s.values.forEachIndexed { i, (_, v) ->
                        writer.write(if (v is EncodedElement.Primitive) v.value else v.toString())
                        if (i < s.values.lastIndex) writer.writeDelimiter()
                    }
                }
            }
            ArrayFormatSelector.ArrayFormat.EXPANDED -> {
                writer.write("[${elements.size}")
                if (config.delimiter.char != ',') writer.write(config.delimiter.char.toString())
                writer.write("]:")
                elements.forEach { writeElement(it, indent + 1) }
            }
        }
    }

    private fun writeNestedArrayField(key: String, elements: List<EncodedElement>, indent: Int) {
        val format = selectNestedFormat(elements)
        when (format) {
            ArrayFormatSelector.ArrayFormat.INLINE -> {
                writer.writeArrayHeader(key, elements.size, config.delimiter.char)
                if (elements.isNotEmpty()) writer.writeSpace()
                elements.forEachIndexed { i, e ->
                    writer.write((e as EncodedElement.Primitive).value)
                    if (i < elements.lastIndex) writer.writeDelimiter()
                }
            }
            ArrayFormatSelector.ArrayFormat.TABULAR -> {
                val first = elements.first() as EncodedElement.Structure
                val fields = ArrayFormatSelector.getFieldNames(first.descriptor).map { quoteKey(it) }
                writer.writeTabularArrayHeader(key, elements.size, fields, config.delimiter.char)
                elements.filterIsInstance<EncodedElement.Structure>().forEach { s ->
                    writer.writeNewline()
                    writer.writeIndent(indent + 1)
                    s.values.forEachIndexed { i, (_, v) ->
                        writer.write(if (v is EncodedElement.Primitive) v.value else v.toString())
                        if (i < s.values.lastIndex) writer.writeDelimiter()
                    }
                }
            }
            ArrayFormatSelector.ArrayFormat.EXPANDED -> {
                writer.writeArrayHeader(key, elements.size, config.delimiter.char)
                elements.forEach { writeElement(it, indent + 1) }
            }
        }
    }

    private fun selectNestedFormat(elements: List<EncodedElement>): ArrayFormatSelector.ArrayFormat {
        if (elements.all { it is EncodedElement.Primitive }) return ArrayFormatSelector.ArrayFormat.INLINE
        if (elements.all { it is EncodedElement.Structure }) {
            val structures = elements.filterIsInstance<EncodedElement.Structure>()
            if (structures.isNotEmpty()) {
                val first = structures.first()
                val allSame = structures.all { it.descriptor == first.descriptor }
                        && ArrayFormatSelector.allPropertiesArePrimitive(first.descriptor)
                        && structures.all { it.values.map { p -> p.first }.toSet() == first.values.map { p -> p.first }.toSet() }
                if (allSame) return ArrayFormatSelector.ArrayFormat.TABULAR
            }
        }
        return ArrayFormatSelector.ArrayFormat.EXPANDED
    }
}

/** Captures field values or array elements during encoding. */
@OptIn(ExperimentalSerializationApi::class)
private class ElementCapturer(
    private val config: ToonConfiguration,
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val onComplete: (List<Pair<String, ToonArrayEncoder.EncodedElement>>) -> Unit,
) : AbstractEncoder() {

    private val values = mutableListOf<Pair<String, ToonArrayEncoder.EncodedElement>>()
    private var currentIndex = -1
    private var isArray = descriptor.kind == StructureKind.LIST

    private fun add(value: ToonArrayEncoder.EncodedElement) {
        val name = when {
            isArray -> currentIndex.toString()
            currentIndex in 0 until descriptor.elementsCount -> descriptor.getElementName(currentIndex)
            else -> "field$currentIndex"
        }
        values.add(name to value)
    }

    private fun quote(value: String) = StringQuoting.quote(value, StringQuoting.QuotingContext.ARRAY_ELEMENT, config.delimiter.char)

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = false
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean { currentIndex = index; return true }

    override fun encodeNull() { add(ToonArrayEncoder.EncodedElement.Primitive("null")) }
    override fun encodeBoolean(value: Boolean) { add(ToonArrayEncoder.EncodedElement.Primitive(if (value) "true" else "false")) }
    override fun encodeByte(value: Byte) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeShort(value: Short) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeInt(value: Int) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeLong(value: Long) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeFloat(value: Float) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeDouble(value: Double) { add(ToonArrayEncoder.EncodedElement.Primitive(NumberNormalizer.normalize(value))) }
    override fun encodeChar(value: Char) { add(ToonArrayEncoder.EncodedElement.Primitive(quote(value.toString()))) }
    override fun encodeString(value: String) { add(ToonArrayEncoder.EncodedElement.Primitive(quote(value))) }
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) { add(ToonArrayEncoder.EncodedElement.Primitive(quote(enumDescriptor.getElementName(index)))) }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = when (descriptor.kind) {
        StructureKind.LIST -> ElementCapturer(config, serializersModule, descriptor) { add(ToonArrayEncoder.EncodedElement.NestedArray(it.map { (_, v) -> v })) }
        StructureKind.CLASS, StructureKind.OBJECT -> ElementCapturer(config, serializersModule, descriptor) { add(ToonArrayEncoder.EncodedElement.Structure(descriptor, it)) }
        else -> this
    }

    override fun endStructure(descriptor: SerialDescriptor) { onComplete(values) }
}
