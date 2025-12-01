package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runDecodeFixtureTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from whitespace.json fixture - Whitespace tolerance in decoding: surrounding spaces around
 * delimiters and values.
 */
class WhitespaceDecodeTest {

    private val fixture = "whitespace"

    @Serializable
    data class TagsResult(val tags: List<String>)

    @Serializable
    data class ItemsResult(val items: List<String>)

    @Test
    fun `tolerates spaces around commas in inline arrays`() {
        runDecodeFixtureTest<TagsResult>(fixture)
    }

    @Test
    fun `tolerates spaces around pipes in inline arrays`() {
        runDecodeFixtureTest<TagsResult>(fixture)
    }

    @Test
    fun `tolerates spaces around tabs in inline arrays`() {
        runDecodeFixtureTest<TagsResult>(fixture)
    }

    @Test
    fun `tolerates leading and trailing spaces in tabular row values`() {
        @Serializable data class TabularItem(val id: Int, val name: String)
        @Serializable data class TabularResult(val items: List<TabularItem>)

        runDecodeFixtureTest<TabularResult>(fixture)
    }

    @Test
    fun `tolerates spaces around delimiters with quoted values`() {
        runDecodeFixtureTest<ItemsResult>(fixture)
    }

    @Test
    fun `parses empty tokens as empty string`() {
        runDecodeFixtureTest<ItemsResult>(fixture)
    }
}
