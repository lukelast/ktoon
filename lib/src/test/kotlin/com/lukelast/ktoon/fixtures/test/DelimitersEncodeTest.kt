package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from delimiters.json fixture - Delimiter options: tab and pipe delimiters, delimiter-aware quoting.
 */
class DelimitersEncodeTest {

    private val fixture = "delimiters"

    @Test
    fun `encodes primitive arrays with tab delimiter`() {
        @Serializable
        data class Root(val tags: List<String>)

        runFixtureTest<Root>(fixture, "encodes primitive arrays with tab delimiter")
    }

    @Test
    fun `encodes primitive arrays with pipe delimiter`() {
        @Serializable
        data class Root(val tags: List<String>)

        runFixtureTest<Root>(fixture, "encodes primitive arrays with pipe delimiter")
    }

    @Test
    fun `encodes primitive arrays with comma delimiter`() {
        @Serializable
        data class Root(val tags: List<String>)

        runFixtureTest<Root>(fixture, "encodes primitive arrays with comma delimiter")
    }

    @Test
    fun `encodes tabular arrays with tab delimiter`() {
        @Serializable
        data class Item(val sku: String, val qty: Int, val price: Double)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "encodes tabular arrays with tab delimiter")
    }

    @Test
    fun `encodes tabular arrays with pipe delimiter`() {
        @Serializable
        data class Item(val sku: String, val qty: Int, val price: Double)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "encodes tabular arrays with pipe delimiter")
    }

    @Test
    fun `encodes nested arrays with tab delimiter`() {
        @Serializable
        data class Root(val pairs: List<List<String>>)

        runFixtureTest<Root>(fixture, "encodes nested arrays with tab delimiter")
    }

    @Test
    fun `encodes nested arrays with pipe delimiter`() {
        @Serializable
        data class Root(val pairs: List<List<String>>)

        runFixtureTest<Root>(fixture, "encodes nested arrays with pipe delimiter")
    }

    @Test
    fun `encodes root-level array with tab delimiter`() {
        runFixtureTest<List<String>>(fixture, "encodes root-level array with tab delimiter")
    }

    @Test
    fun `encodes root-level array with pipe delimiter`() {
        runFixtureTest<List<String>>(fixture, "encodes root-level array with pipe delimiter")
    }

    @Test
    fun `encodes root-level array of objects with tab delimiter`() {
        @Serializable
        data class Item(val id: Int)

        runFixtureTest<List<Item>>(fixture, "encodes root-level array of objects with tab delimiter")
    }

    @Test
    fun `encodes root-level array of objects with pipe delimiter`() {
        @Serializable
        data class Item(val id: Int)

        runFixtureTest<List<Item>>(fixture, "encodes root-level array of objects with pipe delimiter")
    }

    @Test
    fun `quotes strings containing tab delimiter`() {
        @Serializable
        data class Root(val items: List<String>)

        runFixtureTest<Root>(fixture, "quotes strings containing tab delimiter")
    }

    @Test
    fun `quotes strings containing pipe delimiter`() {
        @Serializable
        data class Root(val items: List<String>)

        runFixtureTest<Root>(fixture, "quotes strings containing pipe delimiter")
    }

    @Test
    fun `does not quote commas with tab delimiter`() {
        @Serializable
        data class Root(val items: List<String>)

        runFixtureTest<Root>(fixture, "does not quote commas with tab delimiter")
    }

    @Test
    fun `does not quote commas with pipe delimiter`() {
        @Serializable
        data class Root(val items: List<String>)

        runFixtureTest<Root>(fixture, "does not quote commas with pipe delimiter")
    }

    @Test
    fun `quotes tabular values containing comma delimiter`() {
        @Serializable
        data class Item(val id: Int, val note: String)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "quotes tabular values containing comma delimiter")
    }

    @Test
    fun `does not quote commas in tabular values with tab delimiter`() {
        @Serializable
        data class Item(val id: Int, val note: String)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "does not quote commas in tabular values with tab delimiter")
    }

    @Test
    fun `does not quote commas in object values with pipe delimiter`() {
        @Serializable
        data class Root(val note: String)

        runFixtureTest<Root>(fixture, "does not quote commas in object values with pipe delimiter")
    }

    @Test
    fun `does not quote commas in object values with tab delimiter`() {
        @Serializable
        data class Root(val note: String)

        runFixtureTest<Root>(fixture, "does not quote commas in object values with tab delimiter")
    }

    @Test
    fun `quotes nested array values containing pipe delimiter`() {
        @Serializable
        data class Root(val pairs: List<List<String>>)

        runFixtureTest<Root>(fixture, "quotes nested array values containing pipe delimiter")
    }

    @Test
    fun `quotes nested array values containing tab delimiter`() {
        @Serializable
        data class Root(val pairs: List<List<String>>)

        runFixtureTest<Root>(fixture, "quotes nested array values containing tab delimiter")
    }

    @Test
    fun `preserves ambiguity quoting regardless of delimiter`() {
        @Serializable
        data class Root(val items: List<String>)

        runFixtureTest<Root>(fixture, "preserves ambiguity quoting regardless of delimiter")
    }
}
