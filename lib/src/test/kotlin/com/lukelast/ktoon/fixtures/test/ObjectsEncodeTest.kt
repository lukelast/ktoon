package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
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
        @Serializable
        data class Root(val id: Int, val name: String, val active: Boolean)

        runFixtureTest<Root>(fixture, "preserves key order in objects")
    }

    @Test
    fun `encodes null values in objects`() {
        @Serializable
        data class Root(val id: Int, val value: String?)

        runFixtureTest<Root>(fixture, "encodes null values in objects")
    }

    @Test
    fun `encodes empty objects as empty string`() {
        @Serializable
        class Root

        runFixtureTest<Root>(fixture, "encodes empty objects as empty string")
    }

    @Test
    fun `quotes string value with colon`() {
        @Serializable
        data class Root(val note: String)

        runFixtureTest<Root>(fixture, "quotes string value with colon")
    }

    @Test
    fun `quotes string value with comma`() {
        @Serializable
        data class Root(val note: String)

        runFixtureTest<Root>(fixture, "quotes string value with comma")
    }

    @Test
    fun `quotes string value with newline`() {
        @Serializable
        data class Root(val text: String)

        runFixtureTest<Root>(fixture, "quotes string value with newline")
    }

    @Test
    fun `quotes string value with embedded quotes`() {
        @Serializable
        data class Root(val text: String)

        runFixtureTest<Root>(fixture, "quotes string value with embedded quotes")
    }

    @Test
    fun `quotes string value with leading space`() {
        @Serializable
        data class Root(val text: String)

        runFixtureTest<Root>(fixture, "quotes string value with leading space")
    }

    @Test
    fun `quotes string value with only spaces`() {
        @Serializable
        data class Root(val text: String)

        runFixtureTest<Root>(fixture, "quotes string value with only spaces")
    }

    @Test
    fun `quotes string value that looks like true`() {
        @Serializable
        data class Root(val v: String)

        runFixtureTest<Root>(fixture, "quotes string value that looks like true")
    }

    @Test
    fun `quotes string value that looks like number`() {
        @Serializable
        data class Root(val v: String)

        runFixtureTest<Root>(fixture, "quotes string value that looks like number")
    }

    @Test
    fun `quotes string value that looks like negative decimal`() {
        @Serializable
        data class Root(val v: String)

        runFixtureTest<Root>(fixture, "quotes string value that looks like negative decimal")
    }

    @Test
    fun `quotes key with colon`() {
        @Serializable
        data class Root(@SerialName("order:id") val orderId: Int)

        runFixtureTest<Root>(fixture, "quotes key with colon")
    }

    @Test
    fun `quotes key with brackets`() {
        @Serializable
        data class Root(@SerialName("[index]") val index: Int)

        runFixtureTest<Root>(fixture, "quotes key with brackets")
    }

    @Test
    fun `quotes key with braces`() {
        @Serializable
        data class Root(@SerialName("{key}") val key: Int)

        runFixtureTest<Root>(fixture, "quotes key with braces")
    }

    @Test
    fun `quotes key with comma`() {
        @Serializable
        data class Root(@SerialName("a,b") val ab: Int)

        runFixtureTest<Root>(fixture, "quotes key with comma")
    }

    @Test
    fun `quotes key with spaces`() {
        @Serializable
        data class Root(@SerialName("full name") val fullName: String)

        runFixtureTest<Root>(fixture, "quotes key with spaces")
    }

    @Test
    fun `quotes key with leading hyphen`() {
        @Serializable
        data class Root(@SerialName("-lead") val lead: Int)

        runFixtureTest<Root>(fixture, "quotes key with leading hyphen")
    }

    @Test
    fun `quotes key with leading and trailing spaces`() {
        @Serializable
        data class Root(@SerialName(" a ") val a: Int)

        runFixtureTest<Root>(fixture, "quotes key with leading and trailing spaces")
    }

    @Test
    fun `quotes numeric key`() {
        @Serializable
        data class Root(@SerialName("123") val key123: String)

        runFixtureTest<Root>(fixture, "quotes numeric key")
    }

    @Test
    fun `quotes empty string key`() {
        @Serializable
        data class Root(@SerialName("") val emptyKey: Int)

        runFixtureTest<Root>(fixture, "quotes empty string key")
    }

    @Test
    fun `escapes newline in key`() {
        @Serializable
        data class Root(@SerialName("line\nbreak") val lineBreak: Int)

        runFixtureTest<Root>(fixture, "escapes newline in key")
    }

    @Test
    fun `escapes tab in key`() {
        @Serializable
        data class Root(@SerialName("tab\there") val tabHere: Int)

        runFixtureTest<Root>(fixture, "escapes tab in key")
    }

    @Test
    fun `escapes quotes in key`() {
        @Serializable
        data class Root(@SerialName("he said \"hi\"") val quote: Int)

        runFixtureTest<Root>(fixture, "escapes quotes in key")
    }

    @Test
    fun `encodes deeply nested objects`() {
        @Serializable
        data class C(val c: String)

        @Serializable
        data class B(val b: C)

        @Serializable
        data class Root(val a: B)

        runFixtureTest<Root>(fixture, "encodes deeply nested objects")
    }

    @Test
    fun `encodes empty nested object`() {
        @Serializable
        class User

        @Serializable
        data class Root(val user: User)

        runFixtureTest<Root>(fixture, "encodes empty nested object")
    }
}