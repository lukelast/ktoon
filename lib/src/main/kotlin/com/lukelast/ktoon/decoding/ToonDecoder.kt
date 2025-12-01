package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonDecodingException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Root decoder for TOON format.
 *
 * Converts parsed ToonValue structures back into Kotlin objects using kotlinx.serialization
 * descriptors.
 *
 * @property reader Parser that provides ToonValue structures
 * @property serializersModule Module with contextual and polymorphic serializers
 * @property config Configuration
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonDecoder(
    private val reader: ToonReader,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder() {

    private var rootValue: ToonValue? = null

    /** Decodes a serializable value using the given deserializer. */
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        // Read root value if not already read
        if (rootValue == null) {
            rootValue = reader.readRoot()
        }

        // Create appropriate decoder based on root value type and target type
        return when (val value = rootValue!!) {
            is ToonValue.Object -> {
                // Check if target is a Map
                when (deserializer.descriptor.kind) {
                    StructureKind.MAP ->
                        ToonMapDecoder(value, serializersModule, config)
                            .decodeSerializableValue(deserializer)
                    else ->
                        ToonObjectDecoder(value, serializersModule, config)
                            .decodeSerializableValue(deserializer)
                }
            }
            is ToonValue.Array -> {
                ToonArrayDecoder(value, serializersModule, config)
                    .decodeSerializableValue(deserializer)
            }
            else -> {
                // Primitive at root
                ToonPrimitiveDecoder(value, serializersModule, config)
                    .decodeSerializableValue(deserializer)
            }
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        throw KtoonDecodingException("decodeElementIndex not supported at root level")
    }
}

/** Decoder for primitive TOON values. */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonPrimitiveDecoder(
    private val value: ToonValue,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder() {

    override fun decodeNull(): Nothing? {
        return null
    }

    override fun decodeNotNullMark(): Boolean {
        return value != ToonValue.Null
    }

    override fun decodeBoolean(): Boolean {
        return when (value) {
            is ToonValue.Boolean -> value.value
            is ToonValue.String -> value.value.toBoolean()
            else ->
                throw KtoonDecodingException.typeMismatch(
                    "Boolean",
                    value::class.simpleName ?: "unknown",
                )
        }
    }

    override fun decodeByte(): Byte {
        return decodeNumber().toByte()
    }

    override fun decodeShort(): Short {
        return decodeNumber().toShort()
    }

    override fun decodeInt(): Int {
        return decodeNumber().toInt()
    }

    override fun decodeLong(): Long {
        return decodeNumber().toLong()
    }

    override fun decodeFloat(): Float {
        return decodeNumber().toFloat()
    }

    override fun decodeDouble(): Double {
        return decodeNumber().toDouble()
    }

    override fun decodeChar(): Char {
        return when (value) {
            is ToonValue.String -> {
                if (value.value.length == 1) {
                    value.value[0]
                } else {
                    throw KtoonDecodingException("Expected single character, got '${value.value}'")
                }
            }
            else ->
                throw KtoonDecodingException.typeMismatch(
                    "Char",
                    value::class.simpleName ?: "unknown",
                )
        }
    }

    override fun decodeString(): String {
        return when (value) {
            is ToonValue.String -> value.value
            is ToonValue.Number -> value.value.toString()
            is ToonValue.Boolean -> value.value.toString()
            ToonValue.Null -> "null"
            else ->
                throw KtoonDecodingException.typeMismatch(
                    "String",
                    value::class.simpleName ?: "unknown",
                )
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val enumName = decodeString()
        val index =
            (0 until enumDescriptor.elementsCount).firstOrNull {
                enumDescriptor.getElementName(it) == enumName
            }
        return index ?: throw KtoonDecodingException("Unknown enum value: $enumName")
    }

    private fun decodeNumber(): Number {
        return when (value) {
            is ToonValue.Number -> value.value
            is ToonValue.String -> {
                value.value.toDoubleOrNull()
                    ?: throw KtoonDecodingException("Cannot parse '${value.value}' as number")
            }
            else ->
                throw KtoonDecodingException.typeMismatch(
                    "Number",
                    value::class.simpleName ?: "unknown",
                )
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return CompositeDecoder.DECODE_DONE
    }
}

/** Decoder for TOON objects (structures with named fields). */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonObjectDecoder(
    private val value: ToonValue.Object,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
    private val descriptor: SerialDescriptor? = null,
) : AbstractDecoder() {

    private var currentIndex = 0
    private var currentFieldName: String? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentIndex < descriptor.elementsCount) {
            val fieldName = descriptor.getElementName(currentIndex)
            if (value.properties.containsKey(fieldName)) {
                currentFieldName = fieldName
                return currentIndex++
            }
            // Check if field is optional
            if (descriptor.isElementOptional(currentIndex)) {
                currentIndex++
                continue
            }
            // Required field missing
            throw KtoonDecodingException.missingField(fieldName)
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = decodePrimitiveValue { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodePrimitiveValue { it.decodeByte() }

    override fun decodeShort(): Short = decodePrimitiveValue { it.decodeShort() }

    override fun decodeInt(): Int = decodePrimitiveValue { it.decodeInt() }

    override fun decodeLong(): Long = decodePrimitiveValue { it.decodeLong() }

    override fun decodeFloat(): Float = decodePrimitiveValue { it.decodeFloat() }

    override fun decodeDouble(): Double = decodePrimitiveValue { it.decodeDouble() }

    override fun decodeChar(): Char = decodePrimitiveValue { it.decodeChar() }

    override fun decodeString(): String = decodePrimitiveValue { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodePrimitiveValue { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodePrimitiveValue(decode: (ToonPrimitiveDecoder) -> T): T {
        val fieldName = getCurrentFieldName()
        val fieldValue =
            value.properties[fieldName] ?: throw KtoonDecodingException.missingField(fieldName)

        val decoder = ToonPrimitiveDecoder(fieldValue, serializersModule, config)
        return decode(decoder)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // If currentFieldName is null, we're beginning the root structure itself
        if (currentFieldName == null) {
            return this
        }

        val fieldName = getCurrentFieldName()
        val fieldValue =
            value.properties[fieldName] ?: throw KtoonDecodingException.missingField(fieldName)

        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                if (fieldValue !is ToonValue.Object) {
                    throw KtoonDecodingException.typeMismatch(
                        "Object",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonObjectDecoder(fieldValue, serializersModule, config)
            }
            StructureKind.LIST -> {
                if (fieldValue !is ToonValue.Array) {
                    throw KtoonDecodingException.typeMismatch(
                        "Array",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonArrayDecoder(fieldValue, serializersModule, config)
            }
            StructureKind.MAP -> {
                if (fieldValue !is ToonValue.Object) {
                    throw KtoonDecodingException.typeMismatch(
                        "Map",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonMapDecoder(fieldValue, serializersModule, config)
            }
            else -> this
        }
    }

    private fun getCurrentFieldName(): String {
        return currentFieldName ?: throw KtoonDecodingException("No current field name available")
    }

    override fun decodeNotNullMark(): Boolean {
        val fieldName = getCurrentFieldName()
        val fieldValue = value.properties[fieldName]
        return fieldValue != null && fieldValue != ToonValue.Null
    }
}

/** Decoder for TOON arrays. */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonArrayDecoder(
    private val value: ToonValue.Array,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder() {

    private var currentIndex = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (currentIndex < value.elements.size) {
            currentIndex++
        } else {
            CompositeDecoder.DECODE_DONE
        }
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return value.elements.size
    }

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = decodeCurrentElement { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodeCurrentElement { it.decodeByte() }

    override fun decodeShort(): Short = decodeCurrentElement { it.decodeShort() }

    override fun decodeInt(): Int = decodeCurrentElement { it.decodeInt() }

    override fun decodeLong(): Long = decodeCurrentElement { it.decodeLong() }

    override fun decodeFloat(): Float = decodeCurrentElement { it.decodeFloat() }

    override fun decodeDouble(): Double = decodeCurrentElement { it.decodeDouble() }

    override fun decodeChar(): Char = decodeCurrentElement { it.decodeChar() }

    override fun decodeString(): String = decodeCurrentElement { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodeCurrentElement { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodeCurrentElement(decode: (ToonPrimitiveDecoder) -> T): T {
        val element = getCurrentElement()
        val decoder = ToonPrimitiveDecoder(element, serializersModule, config)
        return decode(decoder)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // If currentIndex is 0, we're beginning the root array itself
        if (currentIndex == 0) {
            return this
        }

        val element = getCurrentElement()

        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                if (element !is ToonValue.Object) {
                    throw KtoonDecodingException.typeMismatch(
                        "Object",
                        element::class.simpleName ?: "unknown",
                    )
                }
                ToonObjectDecoder(element, serializersModule, config)
            }
            StructureKind.LIST -> {
                if (element !is ToonValue.Array) {
                    throw KtoonDecodingException.typeMismatch(
                        "Array",
                        element::class.simpleName ?: "unknown",
                    )
                }
                ToonArrayDecoder(element, serializersModule, config)
            }
            else -> this
        }
    }

    private fun getCurrentElement(): ToonValue {
        // currentIndex was already incremented by decodeElementIndex
        val index = currentIndex - 1
        if (index < 0 || index >= value.elements.size) {
            throw KtoonDecodingException("Array index out of bounds: $index")
        }
        return value.elements[index]
    }

    override fun decodeNotNullMark(): Boolean {
        val element = getCurrentElement()
        return element != ToonValue.Null
    }
}

/**
 * Decoder for TOON objects as Maps.
 *
 * Handles the conversion of TOON objects to Map structures, where keys are property names
 * and values are property values. kotlinx.serialization expects alternating key-value pairs
 * with sequential indices.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonMapDecoder(
    private val value: ToonValue.Object,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder() {

    private val entries = value.properties.entries.toList()
    private var currentIndex = 0
    private var currentEntry: Map.Entry<String, ToonValue>? = null

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return entries.size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // Maps are decoded as alternating key-value pairs
        // Even indices (0, 2, 4, ...) = keys
        // Odd indices (1, 3, 5, ...) = values
        if (currentIndex >= entries.size * 2) {
            return CompositeDecoder.DECODE_DONE
        }
        val result = currentIndex
        val entryIndex = currentIndex / 2
        if (currentIndex % 2 == 0) {
            currentEntry = entries[entryIndex]
        }
        currentIndex++
        return result
    }

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = decodePrimitiveValue { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodePrimitiveValue { it.decodeByte() }

    override fun decodeShort(): Short = decodePrimitiveValue { it.decodeShort() }

    override fun decodeInt(): Int = decodePrimitiveValue { it.decodeInt() }

    override fun decodeLong(): Long = decodePrimitiveValue { it.decodeLong() }

    override fun decodeFloat(): Float = decodePrimitiveValue { it.decodeFloat() }

    override fun decodeDouble(): Double = decodePrimitiveValue { it.decodeDouble() }

    override fun decodeChar(): Char = decodePrimitiveValue { it.decodeChar() }

    override fun decodeString(): String {
        val entry = currentEntry ?: throw KtoonDecodingException("No current map entry")
        // For keys (even indices after increment = odd currentIndex), return the key
        // For values (odd indices after increment = even currentIndex), return the value as string
        return if ((currentIndex - 1) % 2 == 0) {
            // Key
            entry.key
        } else {
            // Value - decode as primitive
            decodePrimitiveValue { it.decodeString() }
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodePrimitiveValue { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodePrimitiveValue(decode: (ToonPrimitiveDecoder) -> T): T {
        val entry = currentEntry ?: throw KtoonDecodingException("No current map entry")
        val decoder = ToonPrimitiveDecoder(entry.value, serializersModule, config)
        return decode(decoder)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // If this is the map structure itself, return this decoder
        if (currentEntry == null) {
            return this
        }

        val entry = currentEntry ?: throw KtoonDecodingException("No current map entry")
        val fieldValue = entry.value

        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                if (fieldValue !is ToonValue.Object) {
                    throw KtoonDecodingException.typeMismatch(
                        "Object",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonObjectDecoder(fieldValue, serializersModule, config)
            }
            StructureKind.LIST -> {
                if (fieldValue !is ToonValue.Array) {
                    throw KtoonDecodingException.typeMismatch(
                        "Array",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonArrayDecoder(fieldValue, serializersModule, config)
            }
            StructureKind.MAP -> {
                if (fieldValue !is ToonValue.Object) {
                    throw KtoonDecodingException.typeMismatch(
                        "Map",
                        fieldValue::class.simpleName ?: "unknown",
                    )
                }
                ToonMapDecoder(fieldValue, serializersModule, config)
            }
            else -> this
        }
    }

    override fun decodeNotNullMark(): Boolean {
        val entry = currentEntry ?: return false
        return entry.value != ToonValue.Null
    }
}
