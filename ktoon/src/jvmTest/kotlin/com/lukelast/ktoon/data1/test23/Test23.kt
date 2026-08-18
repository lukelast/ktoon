package com.lukelast.ktoon.data1.test23

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Test23: A map root reached through an inline value class. The wrapper serializes as its
 * underlying map, so the document is an ordinary object (§8) that the CLI and ktoon both encode the
 * same way — the point of the golden is the decode leg, where the top-level serializer's own
 * descriptor is a `CLASS`, not a `MAP`, and the root decoder still has to hand the map deserializer
 * a map-shaped decoder.
 */
class Test23 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

@Serializable @JvmInline value class Inventory(val counts: Map<String, Int>)

val data = Inventory(counts = mapOf("bolts" to 120, "nuts" to 45))
