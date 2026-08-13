package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonEncodingException

/**
 * The property names one map contributes to a TOON object.
 *
 * A map's keys are distinct by definition, but the host-to-text conversion need not be: §2's number
 * rules deliberately fold -0 into 0 and NaN/±Infinity into null (§3). Applying them to keys would
 * emit duplicate sibling keys, which strict decoders reject (§14.3) and non-strict decoders resolve
 * by dropping an entry. Floating keys therefore get reversible spellings, and any remaining
 * collision — from a custom key serializer, say — is reported instead of written out.
 */
internal class MapKeyNames {

    private val used = mutableSetOf<String>()

    /** Registers [name] as this map's next property name, or fails if it is already taken. */
    fun claim(name: String): String {
        if (!used.add(name)) {
            throw KtoonEncodingException(
                "Map keys collapsed to the same TOON property name '$name'"
            )
        }
        return name
    }

    fun format(value: Double): String =
        when {
            value.isNaN() -> NAN_KEY
            value == Double.POSITIVE_INFINITY -> INFINITY_KEY
            value == Double.NEGATIVE_INFINITY -> NEGATIVE_INFINITY_KEY
            value == 0.0 && value.toRawBits() != 0L -> NEGATIVE_ZERO_KEY
            else -> NumberNormalizer.normalize(value)
        }

    fun format(value: Float): String =
        when {
            value.isNaN() -> NAN_KEY
            value == Float.POSITIVE_INFINITY -> INFINITY_KEY
            value == Float.NEGATIVE_INFINITY -> NEGATIVE_INFINITY_KEY
            value == 0.0f && value.toRawBits() != 0 -> NEGATIVE_ZERO_KEY
            else -> NumberNormalizer.normalize(value)
        }

    private companion object {
        const val NAN_KEY = "NaN"
        const val INFINITY_KEY = "Infinity"
        const val NEGATIVE_INFINITY_KEY = "-Infinity"
        const val NEGATIVE_ZERO_KEY = "-0.0"
    }
}
