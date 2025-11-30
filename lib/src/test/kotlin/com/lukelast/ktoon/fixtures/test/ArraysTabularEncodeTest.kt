package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from arrays-tabular.json fixture - Tabular array encoding: arrays of uniform objects with primitive values.
 */
class ArraysTabularEncodeTest {

    private val fixture = "arrays-tabular"

    @Test
    fun `encodes arrays of uniform objects in tabular format`() {
        @Serializable
        data class Item(val sku: String, val qty: Int, val price: Double)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "encodes arrays of uniform objects in tabular format")
    }

    @Test
    fun `encodes null values in tabular format`() {
        @Serializable
        data class Item(val id: Int, val value: String?)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "encodes null values in tabular format")
    }

    @Test
    fun `quotes strings containing delimiters in tabular rows`() {
        @Serializable
        data class Item(val sku: String, val desc: String, val qty: Int)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "quotes strings containing delimiters in tabular rows")
    }

    @Test
    fun `quotes ambiguous strings in tabular rows`() {
        @Serializable
        data class Item(val id: Int, val status: String)

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "quotes ambiguous strings in tabular rows")
    }

    @Test
    fun `encodes tabular arrays with keys needing quotes`() {
        @Serializable
        data class Item(
            @SerialName("order:id") val orderId: Int,
            @SerialName("full name") val fullName: String
        )

        @Serializable
        data class Root(val items: List<Item>)

        runFixtureTest<Root>(fixture, "encodes tabular arrays with keys needing quotes")
    }
}
