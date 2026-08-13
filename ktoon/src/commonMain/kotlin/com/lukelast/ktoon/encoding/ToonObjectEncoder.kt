package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.util.isUnsignedDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/** Encoder for TOON objects (structures with named fields). */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ToonObjectEncoder(
    private val rawWriter: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val isRoot: Boolean = false,
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder(), ToonNumberSink {

    override fun encodeNumberLiteral(literal: String) =
        writePrimitiveField(NumberNormalizer.normalizeLiteral(literal))

    private var currentKey: String? = null

    /**
     * Whether a field has been written yet. A separating newline goes before every field but the
     * first *written* one — the descriptor index is not the same thing, because a field can be
     * skipped (a default, or `@EncodeDefault(NEVER)`) and would then leave the document starting
     * with a blank line.
     */
    private var wroteAnyField = false

    // Sorting support: buffer fields when sortFields is enabled
    private val bufferedFields: MutableList<Pair<String, String>>? =
        if (config.sortFields) mutableListOf() else null
    private var fieldWriter: ToonWriter? = null

    private val writer: ToonWriter
        get() = fieldWriter ?: rawWriter

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) =
        config.encodeDefaults

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentKey = descriptor.getElementName(index)

        // Start capturing to field buffer if sorting
        if (bufferedFields != null) {
            val buffer = ToonWriter(config)
            fieldWriter = buffer
            // When sorting, only write indent to buffer (newlines added during sorted write)
            buffer.writeIndent(indentLevel)
            return true
        }

        if (!isRoot || wroteAnyField) {
            rawWriter.writeNewline()
        }
        rawWriter.writeIndent(indentLevel)
        wroteAnyField = true
        return true
    }

    override fun encodeNull() = writePrimitiveField("null")

    override fun encodeBoolean(value: Boolean) = writePrimitiveField(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) =
        writePrimitiveField(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) = writePrimitiveField(quoteValue(value.toString()))

    override fun encodeString(value: String) = writePrimitiveField(quoteValue(value))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        writePrimitiveField(quoteValue(enumDescriptor.getElementName(index)))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { writePrimitiveField(it) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val key = currentKey ?: error("Current key is null for structure start")

        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    // §9.5: capture first so keyed tabular form can be selected from the values
                    ElementCapturer(config, serializersModule, descriptor) { entries ->
                        ElementWriter(writer, config).writeObjectField(key, entries, indentLevel)
                        finishField()
                    }
                } else {
                    writeKey(key)
                    ToonObjectEncoder(
                        rawWriter = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = indentLevel + 1,
                        isRoot = false,
                        onEnd = { finishField() },
                    )
                }
            }
            StructureKind.MAP -> {
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    MapElementCapturer(config, serializersModule) { entries ->
                        ElementWriter(writer, config).writeObjectField(key, entries, indentLevel)
                        finishField()
                    }
                } else {
                    writeKey(key)
                    ToonMapEncoder(
                        writer = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = indentLevel + 1,
                        isRoot = false,
                        onEnd = { finishField() },
                    )
                }
            }
            StructureKind.LIST ->
                ToonArrayEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = indentLevel,
                    key = key,
                    onEnd = { finishField() },
                )
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Write sorted fields to actual writer
        if (bufferedFields != null) {
            val sorted = bufferedFields.sortedBy { it.first }
            sorted.forEachIndexed { index, (_, output) ->
                if (!isRoot || index > 0) rawWriter.writeNewline()
                rawWriter.write(output)
            }
        }
        onEnd?.invoke()
    }

    private fun quoteValue(value: String) =
        StringQuoting.quote(value, StringQuoting.QuotingContext.OBJECT_VALUE, config.delimiter.char)

    private fun quoteKey(key: String) =
        StringQuoting.quote(key, StringQuoting.QuotingContext.OBJECT_KEY, config.delimiter.char)

    private fun writeKey(key: String) = writer.writeKey(quoteKey(key))

    private fun writePrimitiveField(value: String) {
        val key = currentKey
        if (key != null) {
            writer.writeKeyValue(quoteKey(key), value)
        }
        finishField()
    }

    private fun finishField() {
        val fw = fieldWriter ?: return
        val key = currentKey ?: return
        bufferedFields?.add(key to fw.toString())
        fieldWriter = null
    }
}
