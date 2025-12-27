package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests from arrays-objects.json fixture - Arrays of objects encoding: list format for non-uniform
 * objects and complex structures.
 */
class ArraysObjectsEncodeTest {

    private val fixture = "arrays-objects"

    @Test
    fun `uses list format for objects with different fields`() {
        @Serializable data class Item(val id: Int, val name: String, val extra: Boolean? = null)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses list format for objects with nested values`() {
        @Serializable data class Nested(val x: Int)

        @Serializable data class Item(val id: Int, val nested: Nested)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `preserves field order in list items - array first`() {
        @Serializable data class Item(val nums: List<Int>, val name: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `preserves field order in list items - primitive first`() {
        @Serializable data class Item(val name: String, val nums: List<Int>)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses list format for objects containing arrays of arrays`() {
        @Serializable data class Item(val matrix: List<List<Int>>, val name: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses tabular format for nested uniform object arrays`() {
        @Serializable data class User(val id: Int, val name: String)

        @Serializable data class Item(val users: List<User>, val status: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses list format for nested object arrays with mismatched keys`() {
        @Serializable data class User(val id: Int, val name: String? = null)

        @Serializable data class Item(val users: List<User>, val status: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses list format for objects with multiple array fields`() {
        @Serializable data class Item(val nums: List<Int>, val tags: List<String>, val name: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses list format for objects with only array fields`() {
        @Serializable data class Item(val nums: List<Int>, val tags: List<String>)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes objects with empty arrays in list format`() {
        @Serializable data class Item(val name: String, val data: List<String>)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses canonical encoding for multi-field list-item objects with tabular arrays`() {
        @Serializable data class User(val id: Int)

        @Serializable data class Item(val users: List<User>, val note: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses canonical encoding for single-field list-item tabular arrays`() {
        @Serializable data class User(val id: Int, val name: String)

        @Serializable data class Item(val users: List<User>)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `places empty arrays on hyphen line when first`() {
        @Serializable data class Item(val data: List<String>, val name: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    @Ignore
    fun `encodes empty object list items as bare hyphen`() {
        @Serializable data class Root(val items: List<JsonElement>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `uses field order from first object for tabular headers`() {
        @Serializable data class Item(val a: Int, val b: Int, val c: Int)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    @Ignore
    fun `uses list format when one object has nested field`() {
        @Serializable data class Root(val items: List<JsonElement>)

        runFixtureEncodeTest<Root>(fixture)
    }
}
