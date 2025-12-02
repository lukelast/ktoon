package com.lukelast.ktoon.encoding

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal sealed class EncodedElement {
    class Primitive(val value: String) : EncodedElement()

    data class Structure(
        val descriptor: SerialDescriptor,
        val values: List<Pair<String, EncodedElement>>,
    ) : EncodedElement() {

        private val fieldsMask: Long by lazy {
            var mask = 0L
            values.forEach { (name, _) ->
                val index = descriptor.getElementIndex(name)
                if (index != CompositeDecoder.UNKNOWN_NAME) {
                    mask = mask or (1L shl index)
                }
            }
            mask
        }

        fun fieldNamesEqual(other: Structure): Boolean {
            return fieldsMask == other.fieldsMask
        }
    }

    data class NestedArray(val elements: List<EncodedElement>) : EncodedElement()
}
