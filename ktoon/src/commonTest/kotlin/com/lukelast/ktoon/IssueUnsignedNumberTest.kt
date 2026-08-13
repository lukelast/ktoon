package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

/** Regression tests for unsigned number encoding reported in `.workflow/issues`. */
class IssueUnsignedNumberTest {

    private val ktoon = Ktoon()

    @Serializable data class Maxima(val b: UByte, val s: UShort, val i: UInt, val l: ULong)

    private val maxima = Maxima(UByte.MAX_VALUE, UShort.MAX_VALUE, UInt.MAX_VALUE, ULong.MAX_VALUE)

    private val maximaToon = "b: 255\ns: 65535\ni: 4294967295\nl: 18446744073709551615"

    @Test
    fun `unsigned fields encode as positive numbers`() {
        // §2: the emitted token must carry the value's mathematical value, not its backing bits.
        assertEquals(maximaToon, ktoon.encodeToString(maxima))
    }

    @Test
    fun `an unsigned root value encodes as a positive number`() {
        assertEquals("4294967295", ktoon.encodeToString(UInt.MAX_VALUE))
    }

    @Test
    fun `unsigned array elements and map entries encode as positive numbers`() {
        assertEquals("[2]: 4294967295,0", ktoon.encodeToString(listOf(UInt.MAX_VALUE, 0u)))
        assertEquals(
            "\"4294967295\": 18446744073709551615",
            ktoon.encodeToString(mapOf(UInt.MAX_VALUE to ULong.MAX_VALUE)),
        )
    }

    @Serializable data class Row(val id: UInt, val size: ULong)

    @Test
    fun `unsigned cells in a tabular array encode as positive numbers`() {
        val rows = listOf(Row(UInt.MAX_VALUE, ULong.MAX_VALUE), Row(1u, 2u))
        val expected = "[2]{id,size}:\n  4294967295,18446744073709551615\n  1,2"
        assertEquals(expected, ktoon.encodeToString(rows))
    }

    @Test
    fun `unsigned values round-trip`() {
        assertEquals(maxima, ktoon.decodeFromString<Maxima>(maximaToon))
        assertEquals(UInt.MAX_VALUE, ktoon.decodeFromString<UInt>("4294967295"))
        assertEquals(
            listOf(UInt.MAX_VALUE, 0u),
            ktoon.decodeFromString<List<UInt>>("[2]: 4294967295,0"),
        )
        assertEquals(
            mapOf(UInt.MAX_VALUE to ULong.MAX_VALUE),
            ktoon.decodeFromString<Map<UInt, ULong>>("\"4294967295\": 18446744073709551615"),
        )
    }

    @Serializable data class OneUInt(val value: UInt)

    @Test
    fun `negative and out-of-range unsigned input is rejected`() {
        assertFailsWith<KtoonException> { ktoon.decodeFromString<OneUInt>("value: -1") }
        assertFailsWith<KtoonException> { ktoon.decodeFromString<OneUInt>("value: 4294967296") }
    }
}
