package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.encoding.ArrayFormatSelector.ArrayFormat.*
import kotlinx.serialization.ExperimentalSerializationApi

/** Utility for selecting the appropriate TOON array format based on content analysis. */
@OptIn(ExperimentalSerializationApi::class)
internal object ArrayFormatSelector {

    enum class ArrayFormat {
        /**
         * A delimiter seperated list of primitive values.
         */
        INLINE,

        /**
         * A compact table of the same object. This is what TOON is known for.
         */
        TABULAR,

        /**
         * A list of objects where objects can be complex and nested.
         */
        EXPANDED,
    }

    fun selectFormat(elements: List<EncodedElement>): ArrayFormat {
        if(elements.isEmpty()){
            return INLINE
        }
        if (elements.all { it is EncodedElement.Primitive }) {
            return INLINE
        }
        val structures = elements.filterIsInstance<EncodedElement.Structure>()
        if(elements.size != structures.size) {
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
