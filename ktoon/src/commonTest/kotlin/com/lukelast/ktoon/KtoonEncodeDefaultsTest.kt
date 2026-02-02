@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.lukelast.ktoon

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class KtoonEncodeDefaultsTest {

    @Test
    fun `encodeDefaults=true + ALWAYS + default`() =
        assertCase(true, AlwaysDefault(), expected(DEFAULT_VALUE))

    @Test
    fun `encodeDefaults=true + ALWAYS + non-default`() =
        assertCase(true, AlwaysDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + ALWAYS + default`() =
        assertCase(false, AlwaysDefault(), expected(DEFAULT_VALUE))

    @Test
    fun `encodeDefaults=false + ALWAYS + non-default`() =
        assertCase(false, AlwaysDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=true + NEVER + default`() = assertCase(true, NeverDefault(), expected(null))

    @Test
    fun `encodeDefaults=true + NEVER + non-default`() =
        assertCase(true, NeverDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + NEVER + default`() =
        assertCase(false, NeverDefault(), expected(null))

    @Test
    fun `encodeDefaults=false + NEVER + non-default`() =
        assertCase(false, NeverDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=true + PLAIN + default`() =
        assertCase(true, PlainDefault(), expected(DEFAULT_VALUE))

    @Test
    fun `encodeDefaults=true + PLAIN + non-default`() =
        assertCase(true, PlainDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + PLAIN + default`() =
        assertCase(false, PlainDefault(), expected(null))

    @Test
    fun `encodeDefaults=false + PLAIN + non-default`() =
        assertCase(false, PlainDefault(CUSTOM_VALUE), expected(CUSTOM_VALUE))

    private inline fun <reified T> assertCase(encodeDefaults: Boolean, value: T, expected: String) {
        val ktoon = Ktoon { this.encodeDefaults = encodeDefaults }
        val encoded = ktoon.encodeToString(value)
        assertEquals(expected, encoded)
        val decoded = ktoon.decodeFromString<T>(encoded)
        assertEquals(value, decoded)
    }

    private fun expected(value: String?): String = value?.let { "value: $it" } ?: ""
}

private const val DEFAULT_VALUE = "default"
private const val CUSTOM_VALUE = "custom"

@Serializable
private data class AlwaysDefault(@EncodeDefault(Mode.ALWAYS) val value: String = DEFAULT_VALUE)

@Serializable
private data class NeverDefault(@EncodeDefault(Mode.NEVER) val value: String = DEFAULT_VALUE)

@Serializable private data class PlainDefault(val value: String = DEFAULT_VALUE)
