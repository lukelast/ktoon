package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from delimiters.json fixture - Delimiter options: tab and pipe delimiters, delimiter-aware
 * quoting.
 */
class DelimitersEncodeTest {

    private val fixture = "delimiters"

    @Test
    fun `encodes primitive arrays with tab delimiter`() {
        @Serializable data class Root(val tags: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes primitive arrays with pipe delimiter`() {
        @Serializable data class Root(val tags: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes primitive arrays with comma delimiter`() {
        @Serializable data class Root(val tags: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes tabular arrays with tab delimiter`() {
        @Serializable data class Item(val sku: String, val qty: Int, val price: Double)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes tabular arrays with pipe delimiter`() {
        @Serializable data class Item(val sku: String, val qty: Int, val price: Double)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes nested arrays with tab delimiter`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes nested arrays with pipe delimiter`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes root-level array with tab delimiter`() {
        runFixtureEncodeTest<List<String>>(fixture)
    }

    @Test
    fun `encodes root-level array with pipe delimiter`() {
        runFixtureEncodeTest<List<String>>(fixture)
    }

    @Test
    fun `encodes root-level array of objects with tab delimiter`() {
        @Serializable data class Item(val id: Int)

        runFixtureEncodeTest<List<Item>>(fixture)
    }

    @Test
    fun `encodes root-level array of objects with pipe delimiter`() {
        @Serializable data class Item(val id: Int)

        runFixtureEncodeTest<List<Item>>(fixture)
    }

    @Test
    fun `quotes strings containing tab delimiter`() {
        @Serializable data class Root(val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes strings containing pipe delimiter`() {
        @Serializable data class Root(val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `does not quote commas with tab delimiter`() {
        @Serializable data class Root(val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `does not quote commas with pipe delimiter`() {
        @Serializable data class Root(val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes tabular values containing comma delimiter`() {
        @Serializable data class Item(val id: Int, val note: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `does not quote commas in tabular values with tab delimiter`() {
        @Serializable data class Item(val id: Int, val note: String)

        @Serializable data class Root(val items: List<Item>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `does not quote commas in object values with pipe delimiter`() {
        @Serializable data class Root(val note: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `does not quote commas in object values with tab delimiter`() {
        @Serializable data class Root(val note: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes nested array values containing pipe delimiter`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes nested array values containing tab delimiter`() {
        @Serializable data class Root(val pairs: List<List<String>>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `preserves ambiguity quoting regardless of delimiter`() {
        @Serializable data class Root(val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }
}
