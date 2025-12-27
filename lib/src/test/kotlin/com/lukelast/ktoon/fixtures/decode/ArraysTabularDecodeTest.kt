package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from arrays-tabular.json fixture - Tabular array decoding: parsing arrays of uniform
 * objects with headers.
 */
class ArraysTabularDecodeTest {

    private val fixture = "arrays-tabular"

    @Serializable data class TabularItem(val sku: String, val qty: Int, val price: Double)

    @Serializable data class IdValue(val id: Int, val value: String?)

    @Serializable data class IdNote(val id: Int, val note: String)

    @Serializable
    data class OrderKey(
        @SerialName("order:id") val orderId: Int,
        @SerialName("full name") val fullName: String,
    )

    @Serializable data class TabularResult(val items: List<TabularItem>)

    @Serializable data class IdValueResult(val items: List<IdValue>)

    @Serializable data class IdNoteResult(val items: List<IdNote>)

    @Serializable data class OrderKeyResult(val items: List<OrderKey>)

    @Test
    fun `parses tabular arrays of uniform objects`() {
        runFixtureDecodeTest<TabularResult>(fixture)
    }

    @Test
    fun `parses nulls and quoted values in tabular rows`() {
        runFixtureDecodeTest<IdValueResult>(fixture)
    }

    @Test
    fun `parses quoted colon in tabular row as data`() {
        runFixtureDecodeTest<IdNoteResult>(fixture)
    }

    @Test
    fun `parses quoted header keys in tabular arrays`() {
        runFixtureDecodeTest<OrderKeyResult>(fixture)
    }

    @Serializable data class CustomItem(val id: Int, val name: String)

    @Test
    fun `parses quoted key with tabular array format`() {
        @Serializable data class CustomItemResult(val `x-items`: List<CustomItem>)
        runFixtureDecodeTest<CustomItemResult>(fixture)
    }

    @Test
    fun `treats unquoted colon as terminator for tabular rows and start of key-value pair`() {
        @Serializable data class CountResult(val items: List<CustomItem>, val count: Int)
        runFixtureDecodeTest<CountResult>(fixture)
    }
}
