package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonDecodingException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
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

        // Create appropriate decoder based on root value type
        return when (val value = rootValue!!) {
            is ToonValue.Object -> {
                if (deserializer.descriptor.kind == StructureKind.MAP) {
                    ToonMapDecoder(value, serializersModule, config)
                        .decodeSerializableValue(deserializer)
                } else {
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
                ToonPrimitiveDecoder(value, serializersModule).decodeSerializableValue(deserializer)
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

        val decoder = ToonPrimitiveDecoder(fieldValue, serializersModule)
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

        return createDecoderForStructure(descriptor, fieldValue, serializersModule, config, this)
    }

    private fun getCurrentFieldName(): String {
        return currentFieldName ?: throw KtoonDecodingException("No current field name available")
    }

    override fun decodeNotNullMark(): Boolean {
        // If we haven't started decoding fields yet, we are checking the object itself
        if (currentFieldName == null) return true

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
        val decoder = ToonPrimitiveDecoder(element, serializersModule)
        return decode(decoder)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // If currentIndex is 0, we're beginning the root array itself
        if (currentIndex == 0) {
            return this
        }

        val element = getCurrentElement()

        return createDecoderForStructure(descriptor, element, serializersModule, config, this)
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
        // If we haven't started decoding elements yet, we are checking the array itself
        if (currentIndex == 0) return true

        val element = getCurrentElement()
        return element != ToonValue.Null
    }
}

/** Decoder for TOON maps. */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonMapDecoder(
    private val value: ToonValue.Object,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder() {

    private val keys = value.properties.keys.toList()
    private var position = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (position < keys.size * 2) {
            return position++
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return value.properties.size
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (position == 0) {
            return this
        }

        val index = position - 1
        val entryIndex = index / 2
        val isKey = index % 2 == 0
        val key = keys[entryIndex]

        if (isKey) {
            return ToonPrimitiveDecoder(ToonValue.String(key), serializersModule)
        }

        val element = value.properties[key]!!
        return createDecoderForStructure(
            descriptor,
            element,
            serializersModule,
            config,
            ToonPrimitiveDecoder(element, serializersModule),
        )
    }

    override fun decodeNotNullMark(): Boolean {
        // If we haven't started decoding entries yet, we are checking the map itself
        if (position == 0) return true

        val index = position - 1
        val entryIndex = index / 2
        val isKey = index % 2 == 0

        if (isKey) return true

        val key = keys[entryIndex]
        val element = value.properties[key]
        return element != null && element != ToonValue.Null
    }

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = decodeCurrent { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodeCurrent { it.decodeByte() }

    override fun decodeShort(): Short = decodeCurrent { it.decodeShort() }

    override fun decodeInt(): Int = decodeCurrent { it.decodeInt() }

    override fun decodeLong(): Long = decodeCurrent { it.decodeLong() }

    override fun decodeFloat(): Float = decodeCurrent { it.decodeFloat() }

    override fun decodeDouble(): Double = decodeCurrent { it.decodeDouble() }

    override fun decodeChar(): Char = decodeCurrent { it.decodeChar() }

    override fun decodeString(): String = decodeCurrent { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodeCurrent { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodeCurrent(decode: (Decoder) -> T): T {
        val index = position - 1
        val entryIndex = index / 2
        val isKey = index % 2 == 0
        val key = keys[entryIndex]

        val decoder =
            if (isKey) {
                ToonPrimitiveDecoder(ToonValue.String(key), serializersModule)
            } else {
                val element = value.properties[key]!!
                ToonPrimitiveDecoder(element, serializersModule)
            }
        return decode(decoder)
    }
}

internal fun createDecoderForStructure(
    descriptor: SerialDescriptor,
    value: ToonValue,
    serializersModule: SerializersModule,
    config: KtoonConfiguration,
    fallback: CompositeDecoder,
): CompositeDecoder {
    return when (descriptor.kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT -> {
            val target = if (value is ToonValue.Null) ToonValue.Object(emptyMap()) else value
            if (target !is ToonValue.Object) {
                throw KtoonDecodingException.typeMismatch(
                    "Object",
                    target::class.simpleName ?: "unknown",
                )
            }
            ToonObjectDecoder(target, serializersModule, config)
        }
        StructureKind.LIST -> {
            if (value !is ToonValue.Array) {
                throw KtoonDecodingException.typeMismatch(
                    "Array",
                    value::class.simpleName ?: "unknown",
                )
            }
            ToonArrayDecoder(value, serializersModule, config)
        }
        StructureKind.MAP -> {
            val target = if (value is ToonValue.Null) ToonValue.Object(emptyMap()) else value
            if (target !is ToonValue.Object) {
                throw KtoonDecodingException.typeMismatch(
                    "Map",
                    target::class.simpleName ?: "unknown",
                )
            }
            ToonMapDecoder(target, serializersModule, config)
        }
        else -> fallback
    }
}
