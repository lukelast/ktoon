package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from primitives.json fixture - Primitive value encoding: strings, numbers, booleans, null.
 */
class PrimitivesEncodeTest {

    private val fixture = "primitives"

    @Test
    fun `encodes safe strings without quotes`() {
        runFixtureTest<String>(fixture, "encodes safe strings without quotes")
    }

    @Test
    fun `encodes safe string with underscore and numbers`() {
        runFixtureTest<String>(fixture, "encodes safe string with underscore and numbers")
    }

    @Test
    fun `quotes empty string`() {
        runFixtureTest<String>(fixture, "quotes empty string")
    }

    @Test
    fun `quotes string that looks like true`() {
        runFixtureTest<String>(fixture, "quotes string that looks like true")
    }

    @Test
    fun `quotes string that looks like false`() {
        runFixtureTest<String>(fixture, "quotes string that looks like false")
    }

    @Test
    fun `quotes string that looks like null`() {
        runFixtureTest<String>(fixture, "quotes string that looks like null")
    }

    @Test
    fun `quotes string that looks like integer`() {
        runFixtureTest<String>(fixture, "quotes string that looks like integer")
    }

    @Test
    fun `quotes string that looks like negative decimal`() {
        runFixtureTest<String>(fixture, "quotes string that looks like negative decimal")
    }

    @Test
    fun `quotes string that looks like scientific notation`() {
        runFixtureTest<String>(fixture, "quotes string that looks like scientific notation")
    }

    @Test
    fun `quotes string with leading zero`() {
        runFixtureTest<String>(fixture, "quotes string with leading zero")
    }

    @Test
    fun `escapes newline in string`() {
        runFixtureTest<String>(fixture, "escapes newline in string")
    }

    @Test
    fun `escapes tab in string`() {
        runFixtureTest<String>(fixture, "escapes tab in string")
    }

    @Test
    fun `escapes carriage return in string`() {
        runFixtureTest<String>(fixture, "escapes carriage return in string")
    }

    @Test
    fun `escapes backslash in string`() {
        runFixtureTest<String>(fixture, "escapes backslash in string")
    }

    @Test
    fun `quotes string with array-like syntax`() {
        runFixtureTest<String>(fixture, "quotes string with array-like syntax")
    }

    @Test
    fun `quotes string starting with hyphen-space`() {
        runFixtureTest<String>(fixture, "quotes string starting with hyphen-space")
    }

    @Test
    fun `quotes single hyphen as object value`() {
        @Serializable data class Marker(val marker: String)

        runFixtureTest<Marker>(fixture, "quotes single hyphen as object value")
    }

    @Test
    fun `quotes string starting with hyphen as object value`() {
        @Serializable data class Note(val note: String)

        runFixtureTest<Note>(fixture, "quotes string starting with hyphen as object value")
    }

    @Test
    fun `quotes single hyphen in array`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "quotes single hyphen in array")
    }

    @Test
    fun `quotes leading-hyphen string in array`() {
        @Serializable data class Tags(val tags: List<String>)

        runFixtureTest<Tags>(fixture, "quotes leading-hyphen string in array")
    }

    @Test
    fun `quotes string with bracket notation`() {
        runFixtureTest<String>(fixture, "quotes string with bracket notation")
    }

    @Test
    fun `quotes string with brace notation`() {
        runFixtureTest<String>(fixture, "quotes string with brace notation")
    }

    @Test
    fun `encodes Unicode string without quotes`() {
        runFixtureTest<String>(fixture, "encodes Unicode string without quotes")
    }

    @Test
    fun `encodes Chinese characters without quotes`() {
        runFixtureTest<String>(fixture, "encodes Chinese characters without quotes")
    }

    @Test
    fun `encodes emoji without quotes`() {
        runFixtureTest<String>(fixture, "encodes emoji without quotes")
    }

    @Test
    fun `encodes string with emoji and spaces`() {
        runFixtureTest<String>(fixture, "encodes string with emoji and spaces")
    }

    // Number encoding tests

    @Test
    fun `encodes positive integer`() {
        runFixtureTest<Int>(fixture, "encodes positive integer")
    }

    @Test
    fun `encodes decimal number`() {
        runFixtureTest<Double>(fixture, "encodes decimal number")
    }

    @Test
    fun `encodes negative integer`() {
        runFixtureTest<Int>(fixture, "encodes negative integer")
    }

    @Test
    fun `encodes zero`() {
        runFixtureTest<Int>(fixture, "encodes zero")
    }

    @Test
    fun `encodes negative zero as zero`() {
        runFixtureTest<Int>(fixture, "encodes negative zero as zero")
    }

    @Test
    fun `encodes scientific notation as decimal`() {
        runFixtureTest<Int>(fixture, "encodes scientific notation as decimal")
    }

    @Test
    fun `encodes small decimal from scientific notation`() {
        runFixtureTest<Double>(fixture, "encodes small decimal from scientific notation")
    }

    @Test
    fun `encodes large number`() {
        runFixtureTest<Double>(fixture, "encodes large number")
    }

    @Test
    fun `encodes MAX_SAFE_INTEGER`() {
        runFixtureTest<Long>(fixture, "encodes MAX_SAFE_INTEGER")
    }

    @Test
    fun `encodes repeating decimal with full precision`() {
        runFixtureTest<Double>(fixture, "encodes repeating decimal with full precision")
    }

    // Boolean and null encoding tests

    @Test
    fun `encodes true`() {
        runFixtureTest<Boolean>(fixture, "encodes true")
    }

    @Test
    fun `encodes false`() {
        runFixtureTest<Boolean>(fixture, "encodes false")
    }

    @Test
    fun `encodes null`() {
        runFixtureTest<String?>(fixture, "encodes null")
    }
}
