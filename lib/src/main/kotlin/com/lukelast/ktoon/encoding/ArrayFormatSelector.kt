package com.lukelast.ktoon.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/** Utility for selecting the appropriate TOON array format based on content analysis. */
@OptIn(ExperimentalSerializationApi::class)
internal object ArrayFormatSelector {

    private const val MAX_INLINE_LENGTH = 80
    private const val MIN_TABULAR_ELEMENTS = 2

    enum class ArrayFormat { INLINE, TABULAR, EXPANDED }

    fun selectFormat(elementDescriptor: SerialDescriptor, estimatedCount: Int = -1): ArrayFormat {
        if (isPrimitiveType(elementDescriptor)) {
            val estimatedLength = if (estimatedCount > 0) estimatedCount * 10 else 0
            return if (estimatedLength <= MAX_INLINE_LENGTH) ArrayFormat.INLINE else ArrayFormat.EXPANDED
        }
        if (isTabularCandidate(elementDescriptor, estimatedCount)) return ArrayFormat.TABULAR
        return ArrayFormat.EXPANDED
    }

    fun isPrimitiveType(descriptor: SerialDescriptor): Boolean = descriptor.kind is PrimitiveKind

    private fun isTabularCandidate(descriptor: SerialDescriptor, estimatedCount: Int): Boolean {
        if (estimatedCount in 0 until MIN_TABULAR_ELEMENTS) return false
        if (descriptor.kind != StructureKind.CLASS && descriptor.kind != StructureKind.OBJECT) return false
        if (descriptor.elementsCount == 0) return false
        return (0 until descriptor.elementsCount).all { isPrimitiveType(descriptor.getElementDescriptor(it)) }
    }

    fun getFieldNames(descriptor: SerialDescriptor): List<String> =
        (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

    fun allPropertiesArePrimitive(descriptor: SerialDescriptor): Boolean =
        (0 until descriptor.elementsCount).all { isPrimitiveType(descriptor.getElementDescriptor(it)) }
}
