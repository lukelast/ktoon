package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from primitives.json fixture - Primitive value encoding: strings, numbers, booleans, null.
 */
class PrimitivesEncodeTest {

    private val fixture = "primitives"

    @Test
    fun `encodes safe strings without quotes`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `encodes safe string with underscore and numbers`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes empty string`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like true`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like false`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like null`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like integer`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like negative decimal`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string that looks like scientific notation`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string with leading zero`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `escapes newline in string`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `escapes tab in string`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `escapes carriage return in string`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `escapes backslash in string`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string with array-like syntax`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string starting with hyphen-space`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes single hyphen as object value`() {
        @Serializable data class Marker(val marker: String)

        runFixtureEncodeTest<Marker>(fixture)
    }

    @Test
    fun `quotes string starting with hyphen as object value`() {
        @Serializable data class Note(val note: String)

        runFixtureEncodeTest<Note>(fixture)
    }

    @Test
    fun `quotes single hyphen in array`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `quotes leading-hyphen string in array`() {
        @Serializable data class Tags(val tags: List<String>)

        runFixtureEncodeTest<Tags>(fixture)
    }

    @Test
    fun `quotes string with bracket notation`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `quotes string with brace notation`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `encodes Unicode string without quotes`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `encodes Chinese characters without quotes`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `encodes emoji without quotes`() {
        runFixtureEncodeTest<String>(fixture)
    }

    @Test
    fun `encodes string with emoji and spaces`() {
        runFixtureEncodeTest<String>(fixture)
    }

    // Number encoding tests

    @Test
    fun `encodes positive integer`() {
        runFixtureEncodeTest<Int>(fixture)
    }

    @Test
    fun `encodes decimal number`() {
        runFixtureEncodeTest<Double>(fixture)
    }

    @Test
    fun `encodes negative integer`() {
        runFixtureEncodeTest<Int>(fixture)
    }

    @Test
    fun `encodes zero`() {
        runFixtureEncodeTest<Int>(fixture)
    }

    @Test
    fun `encodes negative zero as zero`() {
        runFixtureEncodeTest<Int>(fixture)
    }

    @Test
    fun `encodes scientific notation as decimal`() {
        runFixtureEncodeTest<Int>(fixture)
    }

    @Test
    fun `encodes small decimal from scientific notation`() {
        runFixtureEncodeTest<Double>(fixture)
    }

    @Test
    fun `encodes large number`() {
        runFixtureEncodeTest<Double>(fixture)
    }

    @Test
    fun `encodes MAX_SAFE_INTEGER`() {
        runFixtureEncodeTest<Long>(fixture)
    }

    @Test
    fun `encodes repeating decimal with full precision`() {
        runFixtureEncodeTest<Double>(fixture)
    }

    // Boolean and null encoding tests

    @Test
    fun `encodes true`() {
        runFixtureEncodeTest<Boolean>(fixture)
    }

    @Test
    fun `encodes false`() {
        runFixtureEncodeTest<Boolean>(fixture)
    }

    @Test
    fun `encodes null`() {
        runFixtureEncodeTest<String?>(fixture)
    }
}
