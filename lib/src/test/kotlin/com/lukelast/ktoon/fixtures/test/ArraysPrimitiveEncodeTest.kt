package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Tests from arrays-primitive.json fixture - Primitive array encoding: inline arrays of strings, numbers, booleans.
 */
class ArraysPrimitiveEncodeTest {

    private val fixture = "arrays-primitive"

    @Test
    fun `encodes string arrays inline`() {
        @Serializable
        data class Tags(val tags: List<String>)

        runFixtureTest<Tags>(fixture, "encodes string arrays inline")
    }

    @Test
    fun `encodes number arrays inline`() {
        @Serializable
        data class Nums(val nums: List<Int>)

        runFixtureTest<Nums>(fixture, "encodes number arrays inline")
    }

    @Test
    @Disabled
    fun `encodes mixed primitive arrays inline`() {
        @Serializable
        data class Data(val data: List<JsonElement>)

        runFixtureTest<Data>(fixture, "encodes mixed primitive arrays inline")
    }

    @Test
    fun `encodes empty arrays`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "encodes empty arrays")
    }

    @Test
    fun `encodes empty string in single-item array`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "encodes empty string in single-item array")
    }

    @Test
    fun `encodes empty string in multi-item array`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "encodes empty string in multi-item array")
    }

    @Test
    fun `encodes whitespace-only strings in arrays`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "encodes whitespace-only strings in arrays")
    }

    @Test
    fun `quotes array strings with comma`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "quotes array strings with comma")
    }

    @Test
    fun `quotes strings that look like booleans in arrays`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "quotes strings that look like booleans in arrays")
    }

    @Test
    fun `quotes strings with structural meanings in arrays`() {
        @Serializable
        data class Items(val items: List<String>)

        runFixtureTest<Items>(fixture, "quotes strings with structural meanings in arrays")
    }
}
