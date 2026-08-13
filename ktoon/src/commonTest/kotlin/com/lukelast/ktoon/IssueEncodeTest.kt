package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
