package com.lukelast.ktoon.data1.test24

import com.lukelast.ktoon.KtoonException
import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

/**
 * Test24: Strings that look like other types. §7.2 makes the encoder quote a string equal to
 * `true`, `false`, or a number, and §4 keeps a quoted token a string on the way back — so the
 * golden round-trips as strings, and decoding that same document into a schema that declares those
 * fields `Boolean` or `Int` is a type mismatch rather than a silent conversion.
 */
class Test24 : AbstractGoldenTest() {
    override fun verify() {
        assertGolden(data)
        assertFailsWith<KtoonException>("quoted booleans decoded as Boolean") {
            ktoon.decodeFromString<BooleanFlags>(goldenToon())
        }
        assertFailsWith<KtoonException>("quoted number decoded as Int") {
            ktoon.decodeFromString<IntCount>(goldenToon())
        }
    }
}

@Serializable data class Flags(val enabled: String, val verbose: String, val count: String)

@Serializable data class BooleanFlags(val enabled: Boolean, val verbose: Boolean)

@Serializable data class IntCount(val count: Int)

val data = Flags(enabled = "true", verbose = "false", count = "42")
