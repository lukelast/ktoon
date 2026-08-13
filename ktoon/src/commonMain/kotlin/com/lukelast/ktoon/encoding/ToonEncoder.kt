package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.util.isObjectKind
import com.lukelast.ktoon.util.isUnsignedDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/** Root encoder for TOON format. */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class ToonEncoder(
    private val writer: ToonWriter,
    private val config: KtoonConfiguration,
    override val serializersModule: SerializersModule,
) : AbstractEncoder(), ToonNumberSink {

    override fun encodeNumberLiteral(literal: String) =
        writer.write(NumberNormalizer.normalizeLiteral(literal))

    override fun encodeNull() = writer.write("null")

    override fun encodeBoolean(value: Boolean) = writer.write(if (value) "true" else "false")

    override fun encodeByte(value: Byte) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeShort(value: Short) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeInt(value: Int) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeLong(value: Long) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeFloat(value: Float) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeDouble(value: Double) = writer.write(NumberNormalizer.normalize(value))

    override fun encodeChar(value: Char) = writeRootText(value.toString())

    override fun encodeString(value: String) = writeRootText(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        writeRootText(enumDescriptor.getElementName(index))

    /** Writes a primitive that is the whole document, where root-only quoting rules apply. */
    private fun writeRootText(value: String) =
        writer.write(
            StringQuoting.quote(
                value,
                StringQuoting.QuotingContext.ROOT_VALUE,
                config.delimiter.char,
            )
        )

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (isUnsignedDescriptor(descriptor)) {
            UnsignedNumberEncoder(serializersModule) { writer.write(it) }
        } else {
            super.encodeInline(descriptor)
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when {
            descriptor.isObjectKind() -> {
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    // §9.5: capture first so keyed tabular form can be selected from the values
                    ElementCapturer(config, serializersModule, descriptor) { entries ->
                        ElementWriter(writer, config).writeRootObject(entries)
                    }
                } else {
                    ToonObjectEncoder(
                        rawWriter = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = 0,
                        isRoot = true,
                    )
                }
            }
            descriptor.kind == StructureKind.MAP ->
                if (ElementWriter.couldBeKeyed(descriptor)) {
                    MapElementCapturer(config, serializersModule) { entries ->
                        ElementWriter(writer, config).writeRootObject(entries)
                    }
                } else {
                    ToonMapEncoder(
                        writer = writer,
                        config = config,
                        serializersModule = serializersModule,
                        indentLevel = 0,
                        isRoot = true,
                    )
                }
            descriptor.kind == StructureKind.LIST ->
                ToonArrayEncoder(
                    writer = writer,
                    config = config,
                    serializersModule = serializersModule,
                    indentLevel = 0,
                    key = null,
                )
            else -> this
        }

    override fun endStructure(descriptor: SerialDescriptor) = Unit
}
