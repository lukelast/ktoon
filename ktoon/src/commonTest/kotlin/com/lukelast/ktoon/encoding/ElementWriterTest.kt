package com.lukelast.ktoon.encoding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Regression tests for the §9 form-detection helpers of [ElementWriter]. */
class ElementWriterTest {

    private fun structure(vararg names: String): EncodedElement.Structure =
        EncodedElement.Structure(names.map { it to EncodedElement.Primitive("1") })

    @Test
    fun `an element repeating a field name is not tabular`() {
        // §9.3: all objects must share one key set, and a repeated name means one is missing.
        assertNull(ElementWriter.tabularTree(listOf(structure("a", "b"), structure("a", "a"))))
    }

    @Test
    fun `an entry value repeating a field name is not keyed tabular`() {
        val entries =
            listOf<Pair<String, EncodedElement>>(
                "x" to structure("a", "b"),
                "y" to structure("a", "a"),
            )
        assertNull(ElementWriter.keyedTree(entries))
    }

    @Test
    fun `a nested column repeating a field name is not tabular`() {
        val first = EncodedElement.Structure(listOf("n" to structure("a", "b")))
        val second = EncodedElement.Structure(listOf("n" to structure("a", "a")))
        assertNull(ElementWriter.tabularTree(listOf(first, second)))
    }

    @Test
    fun `the same field names in another order stay tabular`() {
        assertNotNull(ElementWriter.tabularTree(listOf(structure("a", "b"), structure("b", "a"))))
    }

    @Test
    fun `a wide table is detected without rescanning each object per field`() {
        // Scanning an object's entries per field made detection quadratic in the field count,
        // which a table this wide turns into hundreds of millions of comparisons.
        val width = 5_000
        val names = List(width) { "f$it" }
        val first = EncodedElement.Structure(names.map { it to EncodedElement.Primitive("1") })
        val second =
            EncodedElement.Structure(names.reversed().map { it to EncodedElement.Primitive("2") })
        val tree = assertNotNull(ElementWriter.tabularTree(listOf(first, second)))
        assertEquals(width, tree.size)
        assertEquals(names, tree.map { it.name })
    }
}
