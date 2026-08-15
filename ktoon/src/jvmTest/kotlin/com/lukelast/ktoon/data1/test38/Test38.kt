package com.lukelast.ktoon.data1.test38

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

/**
 * Test38: Pipe as the document delimiter across every header shape (§11).
 *
 * test14 only covers pipe on a single inline primitive array; this exercises the rest of §11's
 * scope:
 * - §6/§9.3 – tabular header `rows[3|]{a|b|c}:`, with the pipe both inside the bracket segment and
 *   between the field names, and rows whose cells are pipe-joined.
 * - §6/§9.5 – keyed tabular header `scores[2:|]{value|label}:`, where the colon marking the keyed
 *   form comes *before* the delimiter symbol, and each entry row is `entrykey: cell|cell`.
 * - §9.2 – an array of arrays: every inner header carries the delimiter marker, including the empty
 *   one (`- [0|]:`), which is the only place a `[0` header is emitted.
 * - §7.2/§11.1 – delimiter-aware quoting flips with the document delimiter: commas are ordinary
 *   data under pipe and stay unquoted, while pipes must be quoted. Both directions are covered in
 *   an object field value, a tabular row cell, and a keyed entry-row cell.
 * - §9.1 – an empty array in field position is still `tags: []`, with no delimiter marker.
 */
class Test38 : AbstractGoldenTest() {
    override val ktoon = Ktoon { delimiter = KtoonConfiguration.Delimiter.PIPE }

    override fun verify() = assertGolden(data)
}

@Serializable data class Row(val a: String, val b: Int, val c: String)

@Serializable data class Score(val value: Int, val label: String)

@Serializable
data class PipeFormsData(
    /** §11.1: a comma is not the active delimiter, so this field value stays unquoted. */
    val note: String,
    /** §11.1: the document delimiter inside a field value forces quoting. */
    val route: String,
    /** §9.3: tabular rows, with the quoting flip repeated in cell position. */
    val rows: List<Row>,
    /** §9.5: keyed tabular, `[2:|]` header plus `alpha: 3|one, two` entry rows. */
    val scores: Map<String, Score>,
    /** §9.2: nested arrays; the empty inner array is `- [0|]:`. */
    val matrix: List<List<Int>>,
    /** §9.1: empty arrays never grow a delimiter marker. */
    val tags: List<String>,
)

val data =
    PipeFormsData(
        note = "alpha, beta",
        route = "north|south",
        rows =
            listOf(
                Row(a = "red, green", b = 1, c = "up|down"),
                Row(a = "plain", b = 2, c = "steady"),
                Row(a = "left|right", b = 3, c = "x, y"),
            ),
        scores =
            mapOf(
                "alpha" to Score(value = 3, label = "one, two"),
                "beta" to Score(value = 4, label = "up|down"),
            ),
        matrix = listOf(listOf(1, 2), emptyList(), listOf(3, 4, 5)),
        tags = emptyList(),
    )
