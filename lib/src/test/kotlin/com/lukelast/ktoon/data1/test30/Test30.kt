package com.lukelast.ktoon.data1.test30

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test30: Delimiter consistency across nesting levels (§11)
 * Tests that document delimiter is applied consistently:
 * - Arrays at root level
 * - Arrays nested within objects
 * - Arrays at multiple depths
 * Expected: All arrays use the same delimiter (comma by default)
 */
class Test30 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class NestedStructure(
    val tags: List<String>,
    val data: DataLayer
)

@Serializable
data class DataLayer(
    val values: List<Int>,
    val nested: NestedValues
)

@Serializable
data class NestedValues(
    val items: List<String>,
    val matrix: List<List<Int>>
)

@Serializable
data class DelimiterConsistencyData(
    val rootArray: List<String>,
    val rootNumbers: List<Int>,
    val nested: NestedStructure
)

val data = DelimiterConsistencyData(
    rootArray = listOf("a", "b", "c"),
    rootNumbers = listOf(1, 2, 3, 4),
    nested = NestedStructure(
        tags = listOf("tag1", "tag2", "tag3"),
        data = DataLayer(
            values = listOf(10, 20, 30, 40, 50),
            nested = NestedValues(
                items = listOf("x", "y", "z"),
                matrix = listOf(
                    listOf(1, 2),
                    listOf(3, 4),
                    listOf(5, 6)
                )
            )
        )
    )
)
