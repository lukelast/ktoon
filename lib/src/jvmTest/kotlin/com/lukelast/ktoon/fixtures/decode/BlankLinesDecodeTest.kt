package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from blank-lines.json fixture - Blank line handling: strict mode errors on blank lines
 * inside arrays, accepts blank lines outside arrays.
 */
class BlankLinesDecodeTest {

    private val fixture = "blank-lines"

    @Test
    fun `throws on blank line inside list array`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Serializable data class TabularItem(val id: Int)

    @Serializable data class TabularResult(val items: List<TabularItem>)

    @Test
    fun `throws on blank line inside tabular array`() {
        runFixtureDecodeTest<TabularResult>(fixture)
    }

    @Test
    fun `throws on multiple blank lines inside array`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Test
    fun `throws on blank line with spaces inside array`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Serializable data class NestedItem(val inner: List<String>)

    @Serializable data class OuterResult(val outer: List<NestedItem>)

    @Test
    fun `throws on blank line in nested list array`() {
        runFixtureDecodeTest<OuterResult>(fixture)
    }

    @Serializable data class TwoFields(val a: Int, val b: Int)

    @Test
    fun `accepts blank line between root-level fields`() {
        runFixtureDecodeTest<TwoFields>(fixture)
    }

    @Serializable data class SingleField(val a: Int)

    @Test
    fun `accepts trailing newline at end of file`() {
        runFixtureDecodeTest<SingleField>(fixture)
    }

    @Test
    fun `accepts multiple trailing newlines`() {
        runFixtureDecodeTest<SingleField>(fixture)
    }

    @Serializable data class WithArray(val items: List<String>, val b: Int)

    @Test
    fun `accepts blank line after array ends`() {
        runFixtureDecodeTest<WithArray>(fixture)
    }

    @Serializable data class NestedWithBlank(val a: NestedB)

    @Serializable data class NestedB(val b: Int, val c: Int)

    @Test
    fun `accepts blank line between nested object fields`() {
        runFixtureDecodeTest<NestedWithBlank>(fixture)
    }

    @Test
    fun `ignores blank lines inside list array when strict=false`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Serializable data class TabularWithBlank(val id: Int, val name: String)

    @Serializable data class TabularBlankResult(val items: List<TabularWithBlank>)

    @Test
    fun `ignores blank lines inside tabular array when strict=false`() {
        runFixtureDecodeTest<TabularBlankResult>(fixture)
    }

    @Test
    fun `ignores multiple blank lines in arrays when strict=false`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }
}
