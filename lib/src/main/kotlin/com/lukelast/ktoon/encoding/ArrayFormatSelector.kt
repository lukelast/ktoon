package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.encoding.ToonArrayEncoder.EncodedElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor

/** Utility for selecting the appropriate TOON array format based on content analysis. */
@OptIn(ExperimentalSerializationApi::class)
internal object ArrayFormatSelector {

    enum class ArrayFormat {
        INLINE,
        TABULAR,
        EXPANDED,
    }

    fun isPrimitiveType(descriptor: SerialDescriptor): Boolean = descriptor.kind is PrimitiveKind

    fun getFieldNames(descriptor: SerialDescriptor): List<String> =
        (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

    fun allPropertiesArePrimitive(descriptor: SerialDescriptor): Boolean =
        (0 until descriptor.elementsCount).all {
            isPrimitiveType(descriptor.getElementDescriptor(it))
        }

    fun selectFormat(elements: List<EncodedElement>): ArrayFormat {
        if (elements.all { it is EncodedElement.Primitive }) {
            return ArrayFormat.INLINE
        }
        if (elements.all { it is EncodedElement.Structure }) {
            val structures = elements.filterIsInstance<EncodedElement.Structure>()
            if (structures.isNotEmpty()) {
                val first = structures.first()
                val allSame =
                    structures.all { it.descriptor == first.descriptor } &&
                        allPropertiesArePrimitive(first.descriptor) &&
                        structures.all {
                            it.values.map { p -> p.first }.toSet() ==
                                first.values.map { p -> p.first }.toSet()
                        }
                if (allSame) return ArrayFormat.TABULAR
            }
        }
        return ArrayFormat.EXPANDED
    }
}
