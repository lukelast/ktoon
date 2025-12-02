package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from validation-errors.json fixture - Validation errors: length mismatches, invalid
 * escapes, syntax errors, delimiter mismatches.
 */
class ValidationErrorsDecodeTest {
    private val fixture = "validation-errors"

    @Test
    fun `throws on array length mismatch (inline primitives - too many)`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Test
    fun `throws on array length mismatch (list format - too many)`() {
        runFixtureDecodeTest<Map<String, List<Int>>>(fixture)
    }

    @Serializable data class TabularItem(val id: Int, val name: String)

    @Serializable data class TabularResult(val items: List<TabularItem>)

    @Test
    fun `throws on tabular row value count mismatch with header field count`() {
        runFixtureDecodeTest<TabularResult>(fixture)
    }

    @Serializable data class IdOnly(val id: Int)

    @Test
    fun `throws on tabular row count mismatch with header length`() {
        runFixtureDecodeTest<List<IdOnly>>(fixture)
    }

    @Test
    fun `throws on invalid escape sequence`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `throws on unterminated string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `throws on missing colon in key-value context`() {
        @Serializable data class User(val user: String)
        @Serializable data class Root(val a: User)
        runFixtureDecodeTest<Root>(fixture)
    }

    @Test
    fun `throws on two primitives at root depth in strict mode`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `throws on delimiter mismatch (header declares tab, row uses comma)`() {
        runFixtureDecodeTest<Map<String, List<TabularItem>>>(fixture)
    }

    @Test
    fun `throws on mismatched delimiter between bracket and brace fields`() {
        runFixtureDecodeTest<Map<String, List<TabularItem>>>(fixture)
    }
}
