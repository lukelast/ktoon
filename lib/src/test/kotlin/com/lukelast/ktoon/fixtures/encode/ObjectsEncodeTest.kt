package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from objects.json fixture - Object encoding: simple objects, nested objects, key encoding.
 */
class ObjectsEncodeTest {

    private val fixture = "objects"

    @Test
    fun `preserves key order in objects`() {
        @Serializable data class Root(val id: Int, val name: String, val active: Boolean)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes null values in objects`() {
        @Serializable data class Root(val id: Int, val value: String?)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes empty objects as empty string`() {
        @Serializable class Root

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with colon`() {
        @Serializable data class Root(val note: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with comma`() {
        @Serializable data class Root(val note: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with newline`() {
        @Serializable data class Root(val text: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with embedded quotes`() {
        @Serializable data class Root(val text: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with leading space`() {
        @Serializable data class Root(val text: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value with only spaces`() {
        @Serializable data class Root(val text: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value that looks like true`() {
        @Serializable data class Root(val v: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value that looks like number`() {
        @Serializable data class Root(val v: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes string value that looks like negative decimal`() {
        @Serializable data class Root(val v: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with colon`() {
        @Serializable data class Root(@SerialName("order:id") val orderId: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with brackets`() {
        @Serializable data class Root(@SerialName("[index]") val index: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with braces`() {
        @Serializable data class Root(@SerialName("{key}") val key: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with comma`() {
        @Serializable data class Root(@SerialName("a,b") val ab: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with spaces`() {
        @Serializable data class Root(@SerialName("full name") val fullName: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with leading hyphen`() {
        @Serializable data class Root(@SerialName("-lead") val lead: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes key with leading and trailing spaces`() {
        @Serializable data class Root(@SerialName(" a ") val a: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes numeric key`() {
        @Serializable data class Root(@SerialName("123") val key123: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `quotes empty string key`() {
        @Serializable data class Root(@SerialName("") val emptyKey: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `escapes newline in key`() {
        @Serializable data class Root(@SerialName("line\nbreak") val lineBreak: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `escapes tab in key`() {
        @Serializable data class Root(@SerialName("tab\there") val tabHere: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `escapes quotes in key`() {
        @Serializable data class Root(@SerialName("he said \"hi\"") val quote: Int)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes deeply nested objects`() {
        @Serializable data class C(val c: String)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes empty nested object`() {
        @Serializable class User

        @Serializable data class Root(val user: User)

        runFixtureEncodeTest<Root>(fixture)
    }
}
