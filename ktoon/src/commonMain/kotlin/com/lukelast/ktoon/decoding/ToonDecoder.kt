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
 * Implemented by every TOON decoder so format-specific serializers (like
 * [com.lukelast.ktoon.serializers.KtoonJsonElementSerializer]) can read the raw [ToonValue] the
 * decoder is currently positioned on, mirroring how JsonDecoder exposes decodeJsonElement.
 */
internal interface ToonValueSource {
    /** The [ToonValue] the next decode call would consume. */
    fun currentToonValue(): ToonValue
}

/**
 * Root decoder for TOON format.
 *
 * Converts parsed ToonValue structures back into Kotlin objects using kotlinx.serialization
 * descriptors.
 *
 * @property parser Parser that provides ToonValue structures
 * @property serializersModule Module with contextual and polymorphic serializers
 * @property config Configuration
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonDecoder(
    private val parser: ToonParser,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder(), ToonValueSource {

    private var rootValue: ToonValue? = null

    override fun currentToonValue(): ToonValue =
        rootValue ?: parser.readRoot().also { rootValue = it }

    /** Decodes a serializable value using the given deserializer. */
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        // Read root value if not already read
        // Create appropriate decoder based on root value type
        return when (val value = currentToonValue()) {
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

/**
 * Decoder for primitive TOON values.
 *
 * @property isMapKey true when [value] is a map's property name rather than a decoded value. Only
 *   then may text be converted to another primitive type: the parser has already decided the type
 *   of every real value (§4), so converting a string there would undo that decision.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ToonPrimitiveDecoder(
    private val value: ToonValue,
    override val serializersModule: SerializersModule,
    private val isMapKey: Boolean = false,
) : AbstractDecoder(), ToonValueSource {

    override fun currentToonValue(): ToonValue = value

    override fun decodeNull(): Nothing? {
        return null
    }

    override fun decodeNotNullMark(): Boolean {
        return value != ToonValue.Null
    }

    override fun decodeBoolean(): Boolean {
        return when (value) {
            is ToonValue.Boolean -> value.value
            // §2/§4: only the lowercase literals are booleans. `toBoolean` would turn every other
            // token into false, inventing a value the document never carried.
            is ToonValue.String ->
                value.value.toBooleanStrictOrNull()
                    ?: throw KtoonDecodingException("Cannot parse '${value.value}' as Boolean")
            else ->
                throw KtoonDecodingException.typeMismatch(
                    "Boolean",
                    value::class.simpleName ?: "unknown",
                )
        }
    }

    override fun decodeByte(): Byte =
        decodeIntegral(Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong(), "Byte").toByte()

    override fun decodeShort(): Short =
        decodeIntegral(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), "Short").toShort()

    override fun decodeInt(): Int =
        decodeIntegral(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), "Int").toInt()

    override fun decodeLong(): Long = decodeIntegral(Long.MIN_VALUE, Long.MAX_VALUE, "Long")

    /**
     * Reads a whole number of the requested width. The accepted token is measured exactly: host
     * `Number` conversions truncate a fraction and wrap or saturate an out-of-range value, which
     * would hand back a different number from the one the document carried.
     */
    private fun decodeIntegral(min: Long, max: Long, target: String): Long {
        val lexeme =
            when {
                value is ToonValue.Number -> value.lexeme
                value is ToonValue.String && isMapKey -> value.value
                else ->
                    throw KtoonDecodingException.typeMismatch(
                        target,
                        value::class.simpleName ?: "unknown",
                    )
            }
        val exact = if (matchesNumberGrammar(lexeme)) exactIntegralValue(lexeme) else null
        if (exact == null || exact < min || exact > max) {
            throw KtoonDecodingException("Cannot decode '$lexeme' as $target")
        }
        return exact
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
        // §4/§7.4: the parser already separated quoted strings from unquoted numbers, booleans,
        // and null. Rendering one of those as text here would hide a mistyped document.
        return when (value) {
            is ToonValue.String -> value.value
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
        return when {
            value is ToonValue.Number -> value.value
            value is ToonValue.String && isMapKey ->
                value.value.toDoubleOrNull()
                    ?: throw KtoonDecodingException("Cannot parse '${value.value}' as number")
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

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // A root value reaches this decoder directly, so the shape check that nested values get
        // from createDecoderForStructure has to happen here too.
        val kind = descriptor.kind
        if (kind is StructureKind) requireShape(kind, value)
        return this
    }
}

/** Decoder for TOON objects (structures with named fields). */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ToonObjectDecoder(
    private val value: ToonValue.Object,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder(), ToonValueSource {

    private var currentIndex = 0
    private var currentFieldName: String? = null

    override fun currentToonValue(): ToonValue {
        // Before any decodeElementIndex call this decoder stands for the whole object.
        val fieldName = currentFieldName ?: return value
        return value.properties[fieldName] ?: throw KtoonDecodingException.missingField(fieldName)
    }

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

    override fun decodeBoolean(): Boolean = decodeCurrentPrimitive { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodeCurrentPrimitive { it.decodeByte() }

    override fun decodeShort(): Short = decodeCurrentPrimitive { it.decodeShort() }

    override fun decodeInt(): Int = decodeCurrentPrimitive { it.decodeInt() }

    override fun decodeLong(): Long = decodeCurrentPrimitive { it.decodeLong() }

    override fun decodeFloat(): Float = decodeCurrentPrimitive { it.decodeFloat() }

    override fun decodeDouble(): Double = decodeCurrentPrimitive { it.decodeDouble() }

    override fun decodeChar(): Char = decodeCurrentPrimitive { it.decodeChar() }

    override fun decodeString(): String = decodeCurrentPrimitive { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodeCurrentPrimitive { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodeCurrentPrimitive(decode: (ToonPrimitiveDecoder) -> T): T {
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
@Suppress("TooManyFunctions")
internal class ToonArrayDecoder(
    private val value: ToonValue.Array,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder(), ToonValueSource {

    private var currentIndex = 0

    override fun currentToonValue(): ToonValue {
        // Before any decodeElementIndex call this decoder stands for the whole array.
        return if (currentIndex == 0) value else getCurrentElement()
    }

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

    override fun decodeBoolean(): Boolean = decodeCurrentPrimitive { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodeCurrentPrimitive { it.decodeByte() }

    override fun decodeShort(): Short = decodeCurrentPrimitive { it.decodeShort() }

    override fun decodeInt(): Int = decodeCurrentPrimitive { it.decodeInt() }

    override fun decodeLong(): Long = decodeCurrentPrimitive { it.decodeLong() }

    override fun decodeFloat(): Float = decodeCurrentPrimitive { it.decodeFloat() }

    override fun decodeDouble(): Double = decodeCurrentPrimitive { it.decodeDouble() }

    override fun decodeChar(): Char = decodeCurrentPrimitive { it.decodeChar() }

    override fun decodeString(): String = decodeCurrentPrimitive { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodeCurrentPrimitive { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodeCurrentPrimitive(decode: (ToonPrimitiveDecoder) -> T): T {
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
@Suppress("TooManyFunctions")
internal class ToonMapDecoder(
    private val value: ToonValue.Object,
    override val serializersModule: SerializersModule,
    private val config: KtoonConfiguration,
) : AbstractDecoder(), ToonValueSource {

    private val keys = value.properties.keys.toList()
    private var position = 0

    override fun currentToonValue(): ToonValue {
        // Before any decodeElementIndex call this decoder stands for the whole map.
        if (position == 0) return value

        val index = position - 1
        val key = keys[index / 2]
        return if (index % 2 == 0) {
            ToonValue.String(key)
        } else {
            value.properties.getValue(key)
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (position < keys.size * 2) {
            return position++
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return value.properties.size
    }

    @Suppress("ReturnCount")
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (position == 0) {
            return this
        }

        val index = position - 1
        val entryIndex = index / 2
        val isKey = index % 2 == 0
        val key = keys[entryIndex]

        if (isKey) {
            return ToonPrimitiveDecoder(ToonValue.String(key), serializersModule, isMapKey = true)
        }

        val element = value.properties.getValue(key)
        return createDecoderForStructure(
            descriptor,
            element,
            serializersModule,
            config,
            ToonPrimitiveDecoder(element, serializersModule),
        )
    }

    @Suppress("ReturnCount")
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

    override fun decodeBoolean(): Boolean = decodeCurrentPrimitive { it.decodeBoolean() }

    override fun decodeByte(): Byte = decodeCurrentPrimitive { it.decodeByte() }

    override fun decodeShort(): Short = decodeCurrentPrimitive { it.decodeShort() }

    override fun decodeInt(): Int = decodeCurrentPrimitive { it.decodeInt() }

    override fun decodeLong(): Long = decodeCurrentPrimitive { it.decodeLong() }

    override fun decodeFloat(): Float = decodeCurrentPrimitive { it.decodeFloat() }

    override fun decodeDouble(): Double = decodeCurrentPrimitive { it.decodeDouble() }

    override fun decodeChar(): Char = decodeCurrentPrimitive { it.decodeChar() }

    override fun decodeString(): String = decodeCurrentPrimitive { it.decodeString() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return decodeCurrentPrimitive { it.decodeEnum(enumDescriptor) }
    }

    private fun <T> decodeCurrentPrimitive(decode: (Decoder) -> T): T {
        val index = position - 1
        val entryIndex = index / 2
        val isKey = index % 2 == 0
        val key = keys[entryIndex]

        val decoder =
            if (isKey) {
                ToonPrimitiveDecoder(ToonValue.String(key), serializersModule, isMapKey = true)
            } else {
                val element = value.properties.getValue(key)
                ToonPrimitiveDecoder(element, serializersModule)
            }
        return decode(decoder)
    }
}

/**
 * Checks that [value] has the shape the requested structure needs. A null is a real value, not a
 * missing one: substituting an empty object for it would hand back an all-defaults class or an
 * empty map that the document never contained. Nullable types never reach here — kotlinx asks
 * `decodeNotNullMark` first.
 */
private fun requireShape(kind: StructureKind, value: ToonValue): ToonValue {
    val expected =
        when (kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> "Object"
            StructureKind.MAP -> "Map"
            StructureKind.LIST -> "Array"
            else -> return value
        }
    val matches =
        if (kind == StructureKind.LIST) value is ToonValue.Array else value is ToonValue.Object
    if (!matches) {
        throw KtoonDecodingException.typeMismatch(expected, value::class.simpleName ?: "unknown")
    }
    return value
}

@OptIn(ExperimentalSerializationApi::class)
internal fun createDecoderForStructure(
    descriptor: SerialDescriptor,
    value: ToonValue,
    serializersModule: SerializersModule,
    config: KtoonConfiguration,
    fallback: CompositeDecoder,
): CompositeDecoder {
    val kind = descriptor.kind
    if (kind !is StructureKind) return fallback
    val target = requireShape(kind, value)
    return when (kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT ->
            ToonObjectDecoder(target as ToonValue.Object, serializersModule, config)
        StructureKind.LIST -> ToonArrayDecoder(target as ToonValue.Array, serializersModule, config)
        StructureKind.MAP -> ToonMapDecoder(target as ToonValue.Object, serializersModule, config)
        else -> fallback
    }
}
