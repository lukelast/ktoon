package com.lukelast.ktoon.data1.test28

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test28: Deep object nesting (12 levels) with arrays (§12)
 * Tests proper indentation at each level (2-space increments)
 * Includes arrays at multiple depths to test indentation consistency
 */
class Test28 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class Level12(
    val value: String,
    val items: List<Item>
)

@Serializable
data class Item(
    val id: Int,
    val name: String
)

@Serializable
data class Level11(
    val value: String,
    val level12: Level12
)

@Serializable
data class Level10(
    val value: String,
    val tags: List<String>,
    val level11: Level11
)

@Serializable
data class Level9(
    val value: String,
    val level10: Level10
)

@Serializable
data class Level8(
    val value: String,
    val level9: Level9
)

@Serializable
data class Level7(
    val value: String,
    val level8: Level8
)

@Serializable
data class Level6(
    val value: String,
    val numbers: List<Int>,
    val level7: Level7
)

@Serializable
data class Level5(
    val value: String,
    val level6: Level6
)

@Serializable
data class Level4(
    val value: String,
    val level5: Level5
)

@Serializable
data class Level3(
    val value: String,
    val level4: Level4
)

@Serializable
data class Level2(
    val value: String,
    val level3: Level3
)

@Serializable
data class Level1(
    val value: String,
    val level2: Level2
)

@Serializable
data class DeepNestingData(
    val rootValue: String,
    val level1: Level1
)

val data = DeepNestingData(
    rootValue = "depth-0",
    level1 = Level1(
        value = "depth-1",
        level2 = Level2(
            value = "depth-2",
            level3 = Level3(
                value = "depth-3",
                level4 = Level4(
                    value = "depth-4",
                    level5 = Level5(
                        value = "depth-5",
                        level6 = Level6(
                            value = "depth-6",
                            numbers = listOf(1, 2, 3, 4, 5),
                            level7 = Level7(
                                value = "depth-7",
                                level8 = Level8(
                                    value = "depth-8",
                                    level9 = Level9(
                                        value = "depth-9",
                                        level10 = Level10(
                                            value = "depth-10",
                                            tags = listOf("alpha", "beta", "gamma"),
                                            level11 = Level11(
                                                value = "depth-11",
                                                level12 = Level12(
                                                    value = "depth-12",
                                                    items = listOf(
                                                        Item(id = 1, name = "first"),
                                                        Item(id = 2, name = "second")
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)
