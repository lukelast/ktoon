package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonDecodingException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Reads the four unsigned Kotlin types. Their serializers ask for the signed backing bits, so the
 * token is measured as an unsigned whole number and only then reinterpreted — a `ULong` above
 * `Long.MAX_VALUE` never goes through a signed or floating conversion.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class UnsignedNumberDecoder(
    private val value: ToonValue,
    override val serializersModule: SerializersModule,
    private val isMapKey: Boolean = false,
) : AbstractDecoder() {

    override fun decodeByte(): Byte = decodeUnsigned(UByte.MAX_VALUE.toULong(), "UByte").toByte()

    override fun decodeShort(): Short =
        decodeUnsigned(UShort.MAX_VALUE.toULong(), "UShort").toShort()

    override fun decodeInt(): Int = decodeUnsigned(UInt.MAX_VALUE.toULong(), "UInt").toInt()

    override fun decodeLong(): Long = decodeUnsigned(ULong.MAX_VALUE, "ULong").toLong()

    private fun decodeUnsigned(max: ULong, target: String): ULong {
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
        val exact = if (matchesNumberGrammar(lexeme)) exactUnsignedValue(lexeme) else null
        if (exact == null || exact > max) {
            throw KtoonDecodingException("Cannot decode '$lexeme' as $target")
        }
        return exact
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        CompositeDecoder.DECODE_DONE
}
