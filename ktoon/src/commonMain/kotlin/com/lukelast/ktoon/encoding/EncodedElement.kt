package com.lukelast.ktoon.encoding

/**
 * A captured value tree used to select and write the §9 form of a value after all of its content is
 * known. Primitive values are stored fully encoded (quoted/normalized).
 */
internal sealed class EncodedElement {
    class Primitive(val value: String) : EncodedElement()

    /** An object's fields (or a map's entries) in encounter order; names are raw (unquoted). */
    class Structure(val entries: List<Pair<String, EncodedElement>>) : EncodedElement()

    class NestedArray(val elements: List<EncodedElement>) : EncodedElement()
}
