package com.lukelast.ktoon.encoding

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
}
