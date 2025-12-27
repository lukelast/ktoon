package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests from arrays-primitive.json fixture - Primitive array decoding: inline arrays of strings,
 * numbers, booleans, quoted strings.
 */
class ArraysPrimitiveDecodeTest {
    private val fixture = "arrays-primitive"

    @Serializable data class Items(val items: List<String>)

    @Test
    fun `parses string arrays inline`() {
        @Serializable data class Tags(val tags: List<String>)
        runFixtureDecodeTest<Tags>(fixture)
    }

    @Test
    fun `parses number arrays inline`() {
        @Serializable data class Nums(val nums: List<Int>)
        runFixtureDecodeTest<Nums>(fixture)
    }

    @Test
    @Ignore("Requires JsonElement support")
    fun `parses mixed primitive arrays inline`() {
        @Serializable data class Data(val data: List<JsonElement>)
        runFixtureDecodeTest<Data>(fixture)
    }

    @Test
    fun `parses empty arrays`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses single-item array with empty string`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses multi-item array with empty string`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses whitespace-only strings in arrays`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses strings with delimiters in arrays`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses strings that look like primitives when quoted`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses strings with structural tokens in arrays`() {
        runFixtureDecodeTest<Items>(fixture)
    }

    @Test
    fun `parses quoted key with inline array`() {
        @Serializable data class MyKey(@SerialName("my-key") val myKey: List<Int>)
        runFixtureDecodeTest<MyKey>(fixture)
    }

    @Test
    fun `parses quoted key containing brackets with inline array`() {
        @Serializable data class CustomKey(@SerialName("key[test]") val keyTest: List<Int>)
        runFixtureDecodeTest<CustomKey>(fixture)
    }

    @Test
    fun `parses quoted key with empty array`() {
        @Serializable data class CustomKey(@SerialName("x-custom") val xCustom: List<String>)
        runFixtureDecodeTest<CustomKey>(fixture)
    }
}
