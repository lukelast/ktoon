package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from indentation-errors.json fixture - Strict mode indentation validation: non-multiple
 * indentation, tab characters, custom indent sizes.
 */
class IndentationErrorsDecodeTest {
    private val fixture = "indentation-errors"

    @Serializable data class NestedB(val b: Int)

    @Serializable data class NestedA(val a: NestedB)

    @Test
    fun `throws on object field with non-multiple indentation (3 spaces with indent=2)`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `throws on list item with non-multiple indentation (3 spaces with indent=2)`() {
        @Serializable data class ListItem(val id: Int)
        @Serializable data class ListResult(val items: List<ListItem>)
        runFixtureDecodeTest<ListResult>(fixture)
    }

    @Test
    fun `throws on non-multiple indentation with custom indent=4 (3 spaces)`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `accepts correct indentation with custom indent size (4 spaces with indent=4)`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `throws on tab character used in indentation`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `throws on mixed tabs and spaces in indentation`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `throws on tab at start of line`() {
        runFixtureDecodeTest<Map<String, Int>>(fixture)
    }

    @Test
    fun `accepts tabs in quoted string values`() {
        @Serializable data class WithText(val text: String)
        runFixtureDecodeTest<WithText>(fixture)
    }

    @Test
    fun `accepts tabs in quoted keys`() {
        @Serializable data class WithKeyTab(@SerialName("key\ttab") val keyTab: String)
        runFixtureDecodeTest<WithKeyTab>(fixture)
    }

    @Test
    fun `accepts tabs in quoted array elements`() {
        runFixtureDecodeTest<Map<String, List<String>>>(fixture)
    }

    @Test
    fun `accepts non-multiple indentation when strict=false`() {
        runFixtureDecodeTest<NestedA>(fixture)
    }

    @Test
    fun `accepts deeply nested non-multiples when strict=false`() {
        @Serializable data class DeepNestedC(val c: Int)
        @Serializable data class DeepNestedB(val b: DeepNestedC)
        @Serializable data class DeepNestedA(val a: DeepNestedB)
        runFixtureDecodeTest<DeepNestedA>(fixture)
    }

    @Serializable data class TwoFields(val a: Int, val b: Int)

    @Test
    fun `parses empty lines without validation errors`() {
        runFixtureDecodeTest<TwoFields>(fixture)
    }

    @Test
    fun `parses root-level content (0 indentation) as always valid`() {
        @Serializable data class ThreeFields(val a: Int, val b: Int, val c: Int)
        runFixtureDecodeTest<ThreeFields>(fixture)
    }

    @Test
    fun `parses lines with only spaces without validation if empty`() {
        runFixtureDecodeTest<TwoFields>(fixture)
    }
}
