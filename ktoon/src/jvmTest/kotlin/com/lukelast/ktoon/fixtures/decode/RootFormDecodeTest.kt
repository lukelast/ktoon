package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlin.test.Test
import kotlinx.serialization.Serializable

/**
 * Tests from root-form.json fixture - Root form detection: empty document, single primitive,
 * multiple primitives.
 */
class RootFormDecodeTest {

    private val fixture = "root-form"

    /** A property-less class only has identity equality, so use a singleton object instead. */
    @Serializable private object EmptyObject

    @Test
    fun `parses empty document as empty object`() {
        runFixtureDecodeTest<EmptyObject>(fixture)
    }
}
