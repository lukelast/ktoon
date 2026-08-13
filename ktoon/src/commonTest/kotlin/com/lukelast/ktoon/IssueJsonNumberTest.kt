package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals

/** Regression tests for JSON number encoding reported in `.workflow/issues`. */
class IssueJsonNumberTest {

    private val ktoon = Ktoon()

    @Test
    fun `an integer beyond Long keeps every digit`() {
        // §2: the emitted number must carry the value the document had.
        assertEquals("18446744073709551615", ktoon.encodeJsonToToon("18446744073709551615"))
        assertEquals("9223372036854775808", ktoon.encodeJsonToToon("9223372036854775808"))
    }

    @Test
    fun `a high precision fraction keeps every digit`() {
        assertEquals(
            "0.123456789012345678901",
            ktoon.encodeJsonToToon("0.123456789012345678901"),
        )
    }

    @Test
    fun `a literal beyond the floating range is not replaced by null`() {
        assertEquals("1e+400", ktoon.encodeJsonToToon("1e400"))
    }

    @Test
    fun `numbers are still canonicalized`() {
        assertEquals("100", ktoon.encodeJsonToToon("1e2"))
        assertEquals("1.5", ktoon.encodeJsonToToon("1.5000"))
        assertEquals("1", ktoon.encodeJsonToToon("1.0"))
        assertEquals("0", ktoon.encodeJsonToToon("-0"))
        assertEquals("0.000001", ktoon.encodeJsonToToon("1e-6"))
        assertEquals("1e-7", ktoon.encodeJsonToToon("1e-7"))
    }

    @Test
    fun `exact numbers survive in every position`() {
        val big = "18446744073709551615"
        assertEquals("value: $big", ktoon.encodeJsonToToon("""{"value":$big}"""))
        assertEquals("[2]: $big,1", ktoon.encodeJsonToToon("""[$big,1]"""))
        assertEquals(
            "[2]{a,b}:\n  $big,1\n  2,3",
            ktoon.encodeJsonToToon("""[{"a":$big,"b":1},{"a":2,"b":3}]"""),
        )
    }

    @Test
    fun `a json number round-trips through TOON unchanged`() {
        val json = """{"id":18446744073709551615,"ratio":0.123456789012345678901}"""
        assertEquals(
            ktoon.decodeToonToJson(ktoon.encodeJsonToToon(json)).toString(),
            json,
        )
    }
}
