package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KeyFoldingMode
import com.lukelast.ktoon.KtoonConfiguration
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
    private val rawWriter: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
    private val indentLevel: Int,
    private val isRoot: Boolean = false,
    private val pendingKeys: List<String> = emptyList(),
    private val siblingKeys: Set<String> = emptySet(),
    private val onEnd: (() -> Unit)? = null,
) : AbstractEncoder() {

    private var elementIndex = 0
    private var currentKey: String? = null
    private var hasWrittenElement = false

    // Sorting support: buffer fields when sortFields is enabled
    private val sortedFields: MutableList<Pair<String, String>>? =
        if (config.sortFields && pendingKeys.isEmpty()) mutableListOf() else null
    private var fieldWriter: ToonWriter? = null

    private val writer: ToonWriter
        get() = fieldWriter ?: rawWriter

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elementIndex = index
        currentKey = descriptor.getElementName(index)
        hasWrittenElement = true

        // Start capturing to field buffer if sorting
        if (sortedFields != null) {
            fieldWriter = ToonWriter(config)
            // When sorting, only write indent to buffer (newlines added during sorted write)
            fieldWriter!!.writeIndent(indentLevel)
            return true
        }

        if (!isRoot || elementIndex > 0) {
            if (pendingKeys.isEmpty()) rawWriter.writeNewline()
        }
        if (pendingKeys.isEmpty()) rawWriter.writeIndent(indentLevel)
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

    private fun checkCollision(foldedKey: String): Boolean =
        siblingKeys.contains(foldedKey) || siblingKeys.any { it.startsWith("$foldedKey.") }

    private fun canFoldKey(descriptor: SerialDescriptor, key: String): Boolean =
        canFoldKey(key) && descriptor.elementsCount <= 1 && descriptor.kind != StructureKind.LIST

    private fun canFoldKey(key: String): Boolean =
        config.keyFolding == KeyFoldingMode.SAFE &&
            StringQuoting.isIdentifierSegment(key) &&
            pendingKeys.size + 1 <= (config.flattenDepth ?: Int.MAX_VALUE)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val key = currentKey ?: error("Current key is null for structure start")

        // Calculate potential folded key to check for collisions
        val nextPendingKeys = pendingKeys + key
        val foldedKey = nextPendingKeys.joinToString(".")

        // Collision check: if the folded key exists as a sibling, we cannot fold
        val collision = pendingKeys.isNotEmpty() && checkCollision(foldedKey)

        return if (canFoldKey(descriptor, key) && !collision) {
            // Continue folding
            ToonObjectEncoder(
                rawWriter = writer,
                config = config,
                serializersModule = serializersModule,
                indentLevel = indentLevel, // Indent doesn't increase while folding
                isRoot = isRoot, // Preserve root status while folding
                pendingKeys = nextPendingKeys,
                siblingKeys = siblingKeys,
            )
        } else {
            // Cannot fold or decided not to

            // Special case: folding into array header
            // If the next element is a list, and we have pending keys, and the list key is
            // foldable,
            // we can merge the pending keys with the list key and let ToonArrayEncoder handle it.
            if (
                descriptor.kind == StructureKind.LIST &&
                    StringQuoting.isIdentifierSegment(key) &&
                    !collision &&
                    pendingKeys.isNotEmpty()
            ) {
                if (!isRoot || elementIndex > 0) writer.writeNewline()
                writer.writeIndent(indentLevel)

                val fullKey = (pendingKeys + key).joinToString(".")
                ToonArrayEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = indentLevel,
                    key = fullKey,
                    onEnd = { finishField() },
                )
            } else {
                val newIndent = flushPendingKeys()

                // For the new encoder, we need the keys of the new object to check for collisions
                // in *its* children
                val newSiblingKeys =
                    (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()

                if (pendingKeys.isNotEmpty()) writer.writeIndent(newIndent)

                when (descriptor.kind) {
                    StructureKind.CLASS,
                    StructureKind.OBJECT -> {
                        writeKey(key)
                        ToonObjectEncoder(
                            rawWriter = writer,
                            config = config,
                            serializersModule = serializersModule,
                            indentLevel = newIndent + 1,
                            isRoot = false,
                            pendingKeys = emptyList(),
                            siblingKeys = newSiblingKeys,
                            onEnd = { finishField() },
                        )
                    }
                    StructureKind.MAP -> {
                        writeKey(key)
                        ToonMapEncoder(
                            writer = writer,
                            config = config,
                            serializersModule = serializersModule,
                            indentLevel = newIndent + 1,
                            isRoot = false,
                            onEnd = { finishField() },
                        )
                    }
                    StructureKind.LIST ->
                        ToonArrayEncoder(
                            writer = writer,
                            config = config,
                            serializersModule = serializersModule,
                            indentLevel = newIndent,
                            key = key,
                            onEnd = { finishField() },
                        )
                    else -> this
                }
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (pendingKeys.isNotEmpty() && !hasWrittenElement) {
            flushPendingKeys(writeNewline = false)
        }

        // Write sorted fields to actual writer
        if (sortedFields != null) {
            val sorted = sortedFields.sortedBy { it.first }
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

    private fun writeKeyAndValue(value: String) {
        val key = currentKey
        if (key != null) {
            val nextPendingKeys = pendingKeys + key
            val foldedKey = nextPendingKeys.joinToString(".")

            if (canFoldKey(key) && !checkCollision(foldedKey)) {
                // Folded primitive
                if (!isRoot || elementIndex > 0) writer.writeNewline()
                writer.writeIndent(indentLevel)
                writer.writeKeyValue(quoteKey(foldedKey), value)
            } else {
                // Cannot fold, flush pending
                val newIndent = flushPendingKeys()

                if (pendingKeys.isNotEmpty()) {
                    // We just flushed, so we are on a new line.
                    writer.writeIndent(newIndent)
                }

                writer.writeKeyValue(quoteKey(key), value)
            }
        }
        finishField()
    }

    private fun finishField() {
        val fw = fieldWriter ?: return
        val key = currentKey ?: return
        sortedFields?.add(key to fw.toString())
        fieldWriter = null
    }

    private fun flushPendingKeys(writeNewline: Boolean = true): Int {
        if (pendingKeys.isEmpty()) return indentLevel

        if (!isRoot || elementIndex > 0) writer.writeNewline()
        writer.writeIndent(indentLevel)

        val combinedKey = pendingKeys.joinToString(".")
        writer.writeKey(quoteKey(combinedKey))

        if (writeNewline) writer.writeNewline()

        return indentLevel + 1
    }

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
