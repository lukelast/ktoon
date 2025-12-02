package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from root-form.json fixture - Root form detection: empty document, single primitive,
 * multiple primitives.
 */
class RootFormDecodeTest {

    private val fixture = "root-form"

    @Test
    fun `parses empty document as empty object`() {
        @Serializable class EmptyObject
        runFixtureDecodeTest<EmptyObject>(fixture)
    }
}
