package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.encoding.ArrayFormatSelector.ArrayFormat.*
import com.lukelast.ktoon.encoding.ToonArrayEncoder.EncodedElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

/** Utility for selecting the appropriate TOON array format based on content analysis. */
@OptIn(ExperimentalSerializationApi::class)
internal object ArrayFormatSelector {

    enum class ArrayFormat {
        INLINE,
        TABULAR,
        EXPANDED,
    }

    fun getFieldNames(descriptor: SerialDescriptor): List<String> =
        (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

    fun selectFormat(elements: List<EncodedElement>): ArrayFormat {
        if (elements.all { it is EncodedElement.Primitive }) {
            return INLINE
        }
        if (elements.any { it !is EncodedElement.Structure }) {
            return EXPANDED
        }
        val structures = elements.filterIsInstance<EncodedElement.Structure>()
        if (structures.isEmpty()) {
            return EXPANDED
        }
        val first = structures.first()
        if (structures.any { it.descriptor != first.descriptor }) {
            return EXPANDED
        }
        // Check if all values in all structures are primitives
        if (structures.any { s -> s.values.any { (_, v) -> v !is EncodedElement.Primitive } }) {
            return EXPANDED
        }
        if (structures.any { !it.fieldNamesEqual(first) }) {
            return EXPANDED
        }
        return TABULAR
    }
}
