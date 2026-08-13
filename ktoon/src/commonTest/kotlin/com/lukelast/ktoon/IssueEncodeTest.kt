package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Regression tests for encoding issues reported in `.workflow/issues`. */
class IssueEncodeTest {

    @Serializable data class Point(val y: Int, val x: Int)

    @Serializable data class Points(val second: Point, val first: Point)

    private val points = Points(second = Point(y = 4, x = 3), first = Point(y = 2, x = 1))

    @Test
    fun `captured keyed tabular objects honor the sortFields option`() {
        val sorted = Ktoon { sortFields = true }
        val expected =
            """
            [2:]{x,y}:
              first: 1,2
              second: 3,4
            """
                .trimIndent()
        assertEquals(expected, sorted.encodeToString(points))
    }

    @Test
    fun `two to the sixty-third encodes as its own value and not Long MAX_VALUE`() {
        // §2: the emitted number must carry enough precision to decode back unchanged.
        val ktoon = Ktoon()
        assertEquals("9223372036854775808", ktoon.encodeToString(9223372036854775808.0))
        assertEquals("9223372036854775808", ktoon.encodeToString(9223372036854775808.0f))
        assertEquals("-9223372036854775808", ktoon.encodeToString(Long.MIN_VALUE.toDouble()))
        assertEquals("-9223372036854775808", ktoon.encodeToString(Long.MIN_VALUE.toFloat()))
    }

    @Test
    fun `floating map keys that share a normalized value stay distinct`() {
        // A map's keys are distinct, so the encoded property names must be too (§14.3).
        val zeros = Ktoon().encodeToString(linkedMapOf(0.0 to 1, -0.0 to 2))
        assertEquals("\"0\": 1\n\"-0.0\": 2", zeros)

        val nonFinite =
            Ktoon()
                .encodeToString(
                    linkedMapOf(
                        Double.POSITIVE_INFINITY to 1,
                        Double.NEGATIVE_INFINITY to 2,
                        Double.NaN to 3,
                    )
                )
        assertEquals("Infinity: 1\n\"-Infinity\": 2\nNaN: 3", nonFinite)
    }

    @Test
    fun `float map keys that share a normalized value stay distinct`() {
        val floats = linkedMapOf(Float.POSITIVE_INFINITY to 1, Float.NEGATIVE_INFINITY to 2)
        assertEquals("Infinity: 1\n\"-Infinity\": 2", Ktoon().encodeToString(floats))
    }

    @Serializable
    data class SkippedFirst(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val a: String = "d",
        val b: Int = 5,
    )

    @Test
    fun `omitting the first field does not start the document with a blank line`() {
        assertEquals("b: 5", Ktoon().encodeToString(SkippedFirst()))
        assertEquals("a: x\nb: 5", Ktoon().encodeToString(SkippedFirst(a = "x")))
    }

    @Serializable data class OneString(val value: String)

    @Test
    fun `a root string starting with a byte-order mark is quoted`() {
        // §12: a U+FEFF at the very start of a document is its BOM and encoders must not emit one.
        val ktoon = Ktoon()
        assertEquals("\"﻿hello\"", ktoon.encodeToString("﻿hello"))
        assertEquals("﻿hello", ktoon.decodeFromString<String>(ktoon.encodeToString("﻿hello")))
        assertEquals("\"﻿\"", ktoon.encodeToString('﻿'))
    }

    @Test
    fun `a byte-order mark away from the document start still needs no quotes`() {
        val ktoon = Ktoon()
        val nested = OneString("﻿hello")
        assertEquals("value: ﻿hello", ktoon.encodeToString(nested))
        assertEquals(nested, ktoon.decodeFromString<OneString>(ktoon.encodeToString(nested)))
    }

    @Test
    fun `a null map key is reported instead of written as the text null`() {
        val message = "TOON does not support null keys in maps"
        val root =
            assertFailsWith<KtoonEncodingException> { Ktoon().encodeToString(mapOf(null to 1)) }
        assertEquals(message, root.message)
        val nested =
            assertFailsWith<KtoonEncodingException> {
                Ktoon().encodeToString(listOf(mapOf(null to 1), mapOf("a" to 2)))
            }
        assertEquals(message, nested.message)
    }

    @Test
    fun `the string key null and null values still encode`() {
        assertEquals("null: 1", Ktoon().encodeToString(mapOf<String?, Int>("null" to 1)))
        assertEquals("a: null", Ktoon().encodeToString(mapOf<String, Int?>("a" to null)))
    }

    @Serializable(with = SameNameSerializer::class) data class Tag(val id: Int)

    object SameNameSerializer : KSerializer<Tag> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Tag", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Tag) = encoder.encodeString("tag")

        override fun deserialize(decoder: Decoder) = Tag(decoder.decodeString().length)
    }

    @Test
    fun `map keys that collide after conversion are reported`() {
        val colliding = linkedMapOf(Tag(1) to 1, Tag(2) to 2)
        assertFailsWith<KtoonEncodingException> { Ktoon().encodeToString(colliding) }
    }

    @Test
    fun `captured keyed tabular objects keep encounter order by default`() {
        val expected =
            """
            [2:]{y,x}:
              second: 4,3
              first: 2,1
            """
                .trimIndent()
        assertEquals(expected, Ktoon().encodeToString(points))
    }
}
