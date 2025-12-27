package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests from arrays-primitive.json fixture - Primitive array encoding: inline arrays of strings,
 * numbers, booleans.
 */
class ArraysPrimitiveEncodeTest {

    private val fixture = "arrays-primitive"

    @Test
    fun `encodes string arrays inline`() {
        @Serializable data class Tags(val tags: List<String>)

        runFixtureEncodeTest<Tags>(fixture)
    }

    @Test
    fun `encodes number arrays inline`() {
        @Serializable data class Nums(val nums: List<Int>)

        runFixtureEncodeTest<Nums>(fixture)
    }

    @Test
    @Ignore
    fun `encodes mixed primitive arrays inline`() {
        @Serializable data class Data(val data: List<JsonElement>)

        runFixtureEncodeTest<Data>(fixture)
    }

    @Test
    fun `encodes empty arrays`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `encodes empty string in single-item array`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `encodes empty string in multi-item array`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `encodes whitespace-only strings in arrays`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `quotes array strings with comma`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `quotes strings that look like booleans in arrays`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }

    @Test
    fun `quotes strings with structural meanings in arrays`() {
        @Serializable data class Items(val items: List<String>)

        runFixtureEncodeTest<Items>(fixture)
    }
}
