package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.ToonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/** Root encoder for TOON format. */
@OptIn(ExperimentalSerializationApi::class)
internal class ToonEncoder(
    private val writer: ToonWriter,
    private val config: ToonConfiguration,
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {

    override fun encodeNull() = writer.write("null")

    override fun encodeBoolean(value: Boolean) = writer.write(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) =
        writer.write(
            StringQuoting.quote(
                value.toString(),
                StringQuoting.QuotingContext.OBJECT_VALUE,
                config.delimiter.char,
            )
        )

    override fun encodeString(value: String) =
        writer.write(
            StringQuoting.quote(
                value,
                StringQuoting.QuotingContext.OBJECT_VALUE,
                config.delimiter.char,
            )
        )

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        writer.write(
            StringQuoting.quote(
                enumDescriptor.getElementName(index),
                StringQuoting.QuotingContext.OBJECT_VALUE,
                config.delimiter.char,
            )
        )

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT,
            StructureKind.MAP ->
                ToonObjectEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = 0,
                    isRoot = true,
                )
            StructureKind.LIST ->
                ToonArrayEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = 0,
                    key = null,
                )
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) {}
}
