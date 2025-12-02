package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import org.junit.jupiter.api.Test

/**
 * Tests from primitives.json fixture - Primitive value decoding: strings, numbers, booleans, null,
 * unescaping.
 */
class PrimitivesDecodeTest {

    private val fixture = "primitives"

    @Test
    fun `parses safe unquoted string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses unquoted string with underscore and numbers`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses empty quoted string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses quoted string with newline escape`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses quoted string with tab escape`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses quoted string with carriage return escape`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses quoted string with backslash escape`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses quoted string with escaped quotes`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses Unicode string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses Chinese characters`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses emoji`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses string with emoji and spaces`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `parses positive integer`() {
        runFixtureDecodeTest<Int>(fixture)
    }

    @Test
    fun `parses decimal number`() {
        runFixtureDecodeTest<Double>(fixture)
    }

    @Test
    fun `parses negative integer`() {
        runFixtureDecodeTest<Int>(fixture)
    }

    @Test
    fun `parses true`() {
        runFixtureDecodeTest<Boolean>(fixture)
    }

    @Test
    fun `parses false`() {
        runFixtureDecodeTest<Boolean>(fixture)
    }

    @Test
    fun `parses null`() {
        runFixtureDecodeTest<String?>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for true`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for false`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for null`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for integer`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for negative decimal`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for scientific notation`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `respects ambiguity quoting for leading-zero`() {
        runFixtureDecodeTest<String>(fixture)
    }
}
