package com.lukelast.ktoon.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Writes the four unsigned Kotlin types. Their serializers pass the signed backing bits down, so
 * this reinterprets them and hands the positive decimal text to the enclosing encoder's own
 * primitive sink, which keeps field ordering, map key state, and tabular capture intact.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class UnsignedNumberEncoder(
    override val serializersModule: SerializersModule,
    private val sink: (String) -> Unit,
) : AbstractEncoder() {

    override fun encodeByte(value: Byte) = sink(value.toUByte().toString())

    override fun encodeShort(value: Short) = sink(value.toUShort().toString())

    override fun encodeInt(value: Int) = sink(value.toUInt().toString())

    override fun encodeLong(value: Long) = sink(value.toULong().toString())
}
