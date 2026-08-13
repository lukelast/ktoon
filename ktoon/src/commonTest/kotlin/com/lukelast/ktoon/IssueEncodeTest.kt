package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable

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
