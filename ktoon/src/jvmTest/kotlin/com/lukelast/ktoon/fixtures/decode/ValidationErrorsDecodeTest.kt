package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from validation-errors.json fixture - Validation errors: length mismatches, invalid
 * escapes, syntax errors, delimiter mismatches.
 */
class ValidationErrorsDecodeTest {
    private val fixture = "validation-errors"

    @Test
    fun `throws when inline values outnumber the declared length`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Test
    fun `throws when list items outnumber the declared length`() {
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
    fun `throws on row width mismatch when rows use a different delimiter than the active delimiter`() {
        runFixtureDecodeTest<Map<String, List<TabularItem>>>(fixture)
    }

    @Test
    fun `throws on mismatched delimiter between bracket and brace fields`() {
        runFixtureDecodeTest<Map<String, List<TabularItem>>>(fixture)
    }
}
