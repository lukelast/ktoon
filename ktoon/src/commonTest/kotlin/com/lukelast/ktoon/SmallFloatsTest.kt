package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Very small floats and subnormal edge cases (§2), formerly the data1 Test24 golden test.
 *
 * These values cannot be checked byte-for-byte against the reference CLI: below 1e-6 exponent form
 * is a §2 MAY and hosts render doubles differently — the CLI (JS) emits shortest-form exponent
 * (`5e-324`) while ktoon expands plain decimal, from a host spelling that itself varies by platform
 * (`4.9E-324` on the JVM). All spellings are conforming and decode to the same doubles, which is
 * what this test asserts.
 */
class SmallFloatsTest {

    private val ktoon = Ktoon()

    @Serializable
    data class SmallFloats(
        val tinyFloat: Double,
        val subnormal: Double,
        val almostZero: Double,
        val precisionTest: Double,
        val microValue: Double,
        val nanoValue: Double,
        val standardSmall: Double,
        val repeatingDecimal: Double,
    )

    private val data =
        SmallFloats(
            tinyFloat = 0.000001, // 1e-6, the smallest magnitude with mandatory plain form
            subnormal = Double.MIN_VALUE, // 4.9e-324, smallest positive subnormal
            almostZero = 1e-308, // near the smallest normal
            precisionTest = 0.3333333333333333,
            microValue = 1e-9,
            nanoValue = 1e-12,
            standardSmall = 0.0001,
            repeatingDecimal = 1.0 / 3.0,
        )

    /** Verbatim output of `npx @toon-format/cli@4.1.1` for [data] (the old Test24 golden). */
    private val cliToon =
        """
        tinyFloat: 0.000001
        subnormal: 5e-324
        almostZero: 1e-308
        precisionTest: 0.3333333333333333
        microValue: 1e-9
        nanoValue: 1e-12
        standardSmall: 0.0001
        repeatingDecimal: 0.3333333333333333
        """
            .trimIndent()

    @Test
    fun `the reference CLI spelling of small floats decodes to the same values`() {
        assertEquals(data, ktoon.decodeFromString(cliToon))
    }

    @Test
    fun `small floats round-trip through typed encoding`() {
        assertEquals(data, ktoon.decodeFromString(ktoon.encodeToString(data)))
    }

    @Test
    fun `small floats round-trip through json encoding`() {
        val json = Json.encodeToString(data)
        assertEquals(data, ktoon.decodeFromString(ktoon.encodeJsonToToon(json)))
    }
}
