package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests from arrays-nested.json fixture - Nested and mixed array encoding: arrays of arrays, mixed
 * type arrays, root arrays.
 */
class ArraysNestedEncodeTest {
    private val fixture = "arrays-nested"

    @Test
    fun `encodes nested arrays of primitives`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes strings containing delimiters in nested arrays`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes empty inner arrays`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes mixed-length inner arrays`() {
        @Serializable data class Root(val pairs: List<List<Int>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    @Ignore
    fun `encodes root-level primitive array`() {
        runFixtureEncodeTest<List<JsonElement>>(fixture)
    }

    @Test
    fun `encodes root-level array of uniform objects in tabular format`() {
        @Serializable data class Item(val id: Int)

        runFixtureEncodeTest<List<Item>>(fixture)
    }

    @Test
    fun `encodes root-level array of non-uniform objects in list format`() {
        @Serializable data class Item(val id: Int, val name: String? = null)

        runFixtureEncodeTest<List<Item>>(fixture)
    }

    @Test
    @Ignore
    fun `encodes root-level array mixing primitive, object, and array of objects in list format`() {
        runFixtureEncodeTest<List<JsonElement>>(fixture)
    }

    @Test
    fun `encodes root-level arrays of arrays`() {
        runFixtureEncodeTest<List<List<Int>>>(fixture)
    }

    @Test
    fun `encodes empty root-level array`() {
        runFixtureEncodeTest<List<String>>(fixture)
    }

    @Test
    fun `encodes complex nested structure`() {
        @Serializable
        data class User(
            val id: Int,
            val name: String,
            val tags: List<String>,
            val active: Boolean,
            val prefs: List<String>,
        )

        @Serializable data class Root(val user: User)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    @Ignore
    fun `uses list format for arrays mixing primitives and objects`() {
        @Serializable data class Root(val items: List<JsonElement>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    @Ignore
    fun `uses list format for arrays mixing objects and arrays`() {
        @Serializable data class Root(val items: List<JsonElement>)

        runFixtureEncodeTest<Root>(fixture)
    }
}
