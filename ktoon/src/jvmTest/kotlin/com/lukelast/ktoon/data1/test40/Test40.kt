package com.lukelast.ktoon.data1.test40

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test40: Non-default indentSize of 4 spaces (§12) Exercises every indent-sensitive construct at
 * 4-space steps:
 * - Nested objects three levels deep (fields at 4/8/12 spaces)
 * - Tabular array inside a nested object: rows one 4-space level under the header
 * - Inline primitive array at depth (§9.1)
 * - Non-tabular array of objects in list form (§9.4): the `steps` column holds arrays, so tabular
 *   form is disqualified; the `- ` items sit one level under the header and the continuation field
 *   sits one level under the hyphen line per the §10 depth model (12 spaces, not hyphen-aligned
 *   at 10) Expected root: `name: root` at column 0, deepest field `size: 5` at column 12
 */
class Test40 : Runner() {
    override val ktoon = Ktoon { indentSize = 4 }

    override fun run() = doTest(data)
}

@Serializable data class Window(val unit: String, val size: Int)

@Serializable data class Limits(val soft: Int, val hard: Int, val window: Window)

@Serializable data class Row(val id: Int, val label: String)

@Serializable data class Job(val id: Int, val steps: List<String>)

@Serializable
data class Config(
    val mode: String,
    val limits: Limits,
    val scores: List<Int>,
    val rows: List<Row>,
    val jobs: List<Job>,
)

@Serializable data class IndentData(val name: String, val config: Config)

val data =
    IndentData(
        name = "root",
        config =
            Config(
                mode = "fast",
                limits = Limits(soft = 10, hard = 20, window = Window(unit = "sec", size = 5)),
                scores = listOf(1, 2, 3),
                rows = listOf(Row(id = 1, label = "alpha"), Row(id = 2, label = "beta")),
                jobs =
                    listOf(
                        Job(id = 1, steps = listOf("a", "b")),
                        Job(id = 2, steps = listOf("c")),
                    ),
            ),
    )
