package com.lukelast.ktoon.data1.test29

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test29: Nested arrays - arrays of arrays of arrays with objects (§9)
 * Tests three-level array nesting with expanded list format
 * Expected: Proper list markers (-) at appropriate depths
 */
class Test29 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class Point(
    val x: Int,
    val y: Int
)

@Serializable
data class NestedArraysData(
    val matrix: List<List<List<Point>>>
)

val data = NestedArraysData(
    matrix = listOf(
        listOf(
            listOf(
                Point(x = 0, y = 0),
                Point(x = 1, y = 0)
            ),
            listOf(
                Point(x = 0, y = 1),
                Point(x = 1, y = 1)
            )
        ),
        listOf(
            listOf(
                Point(x = 2, y = 0),
                Point(x = 3, y = 0)
            ),
            listOf(
                Point(x = 2, y = 1),
                Point(x = 3, y = 1)
            )
        ),
        listOf(
            listOf(
                Point(x = 4, y = 0),
                Point(x = 5, y = 0)
            )
        )
    )
)
