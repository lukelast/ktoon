package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
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
) : AbstractEncoder() {

    private var elementIndex = 0
    private var currentKey: String? = null

    // Sorting support: buffer fields when sortFields is enabled
    private val bufferedFields: MutableList<Pair<String, String>>? =
        if (config.sortFields) mutableListOf() else null
    private var fieldWriter: ToonWriter? = null

    private val writer: ToonWriter
        get() = fieldWriter ?: rawWriter

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) =
        config.encodeDefaults

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elementIndex = index
        currentKey = descriptor.getElementName(index)

        // Start capturing to field buffer if sorting
        if (bufferedFields != null) {
            val buffer = ToonWriter(config)
            fieldWriter = buffer
            // When sorting, only write indent to buffer (newlines added during sorted write)
            buffer.writeIndent(indentLevel)
            return true
        }

        if (!isRoot || elementIndex > 0) {
            rawWriter.writeNewline()
        }
        rawWriter.writeIndent(indentLevel)
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
