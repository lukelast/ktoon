package com.lukelast.ktoon.data1.test32

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test32: Comprehensive empty and single-element structures (§9)
 * Tests proper encoding of:
 * - Empty strings: ""
 * - Empty arrays: [0]:
 * - Empty tabular arrays: [0]{}:
 * - Single-element arrays of each type
 * - Nested empty structures
 * Expected: Proper encoding of empty markers and single elements
 */
class Test32 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class SingleItem(
    val id: Int,
    val name: String
)

@Serializable
data class EmptyStructuresData(
    val emptyString: String,
    val emptyStringArray: List<String>,
    val emptyIntArray: List<Int>,
    val emptyObjectArray: List<SingleItem>,

    val singleString: List<String>,
    val singleInt: List<Int>,
    val singleObject: List<SingleItem>,

    val normalString: String,
    val normalArray: List<String>,

    val nested: NestedEmpty
)

@Serializable
data class NestedEmpty(
    val emptyInner: List<String>,
    val singleInner: List<Int>,
    val deepNested: DeepEmpty
)

@Serializable
data class DeepEmpty(
    val emptyList: List<String>,
    val emptyTabular: List<SingleItem>
)

val data = EmptyStructuresData(
    emptyString = "",
    emptyStringArray = emptyList(),
    emptyIntArray = emptyList(),
    emptyObjectArray = emptyList(),

    singleString = listOf("only"),
    singleInt = listOf(42),
    singleObject = listOf(SingleItem(id = 1, name = "single")),

    normalString = "normal value",
    normalArray = listOf("a", "b", "c"),

    nested = NestedEmpty(
        emptyInner = emptyList(),
        singleInner = listOf(99),
        deepNested = DeepEmpty(
            emptyList = emptyList(),
            emptyTabular = emptyList()
        )
    )
)
