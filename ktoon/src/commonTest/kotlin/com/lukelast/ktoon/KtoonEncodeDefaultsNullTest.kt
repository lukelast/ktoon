@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.lukelast.ktoon

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class KtoonEncodeDefaultsNullTest {

    @Test
    fun `encodeDefaults=true + ALWAYS + default`() =
        assertCase(true, AlwaysDefaultNull(), line(NULL_LITERAL))

    @Test
    fun `encodeDefaults=true + ALWAYS + non-default`() =
        assertCase(true, AlwaysDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + ALWAYS + default`() =
        assertCase(false, AlwaysDefaultNull(), line(NULL_LITERAL))

    @Test
    fun `encodeDefaults=false + ALWAYS + non-default`() =
        assertCase(false, AlwaysDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=true + NEVER + default`() = assertCase(true, NeverDefaultNull(), omit)

    @Test
    fun `encodeDefaults=true + NEVER + non-default`() =
        assertCase(true, NeverDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + NEVER + default`() = assertCase(false, NeverDefaultNull(), omit)

    @Test
    fun `encodeDefaults=false + NEVER + non-default`() =
        assertCase(false, NeverDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=true + PLAIN + default`() = assertCase(true, PlainDefaultNull(), line(NULL_LITERAL))

    @Test
    fun `encodeDefaults=true + PLAIN + non-default`() =
        assertCase(true, PlainDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    @Test
    fun `encodeDefaults=false + PLAIN + default`() = assertCase(false, PlainDefaultNull(), omit)

    @Test
    fun `encodeDefaults=false + PLAIN + non-default`() =
        assertCase(false, PlainDefaultNull(CUSTOM_VALUE), line(CUSTOM_VALUE))

    private inline fun <reified T> assertCase(
        encodeDefaults: Boolean,
        value: T,
        expected: Expected,
    ) {
        val ktoon = Ktoon { this.encodeDefaults = encodeDefaults }
        val encoded = ktoon.encodeToString(value)
        val expectedText =
            when (expected) {
                is Expected.Line -> "value: ${expected.value}"
                Expected.Omit -> ""
            }
        assertEquals(expectedText, encoded)
        val decoded = ktoon.decodeFromString<T>(encoded)
        assertEquals(value, decoded)
    }
}

private const val CUSTOM_VALUE = "custom"
private const val NULL_LITERAL = "null"

private sealed interface Expected {
    data class Line(val value: String) : Expected

    object Omit : Expected
}

private fun line(value: String) = Expected.Line(value)

private val omit = Expected.Omit

@Serializable
private data class AlwaysDefaultNull(
    @EncodeDefault(Mode.ALWAYS) val value: String? = null,
)

@Serializable
private data class NeverDefaultNull(
    @EncodeDefault(Mode.NEVER) val value: String? = null,
)

@Serializable private data class PlainDefaultNull(val value: String? = null)
