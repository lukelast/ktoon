package com.lukelast.ktoon.data1.test18

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Tests that a nullable root object can be correctly serialized and deserialized.
 * This ensures that ToonDecoder correctly handles nullability checks on the root object before any fields are parsed.
 */
class Test18 : Runner() {
    override fun run() = doTest<SimpleTestData?>(manualData)
}

@Serializable
data class SimpleTestData(
    val id: Int,
    val name: String,
    val active: Boolean
)

private val manualData = SimpleTestData(
    id = 42,
    name = "Test",
    active = true
)
