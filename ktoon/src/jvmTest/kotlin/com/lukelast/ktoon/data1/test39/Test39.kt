package com.lukelast.ktoon.data1.test39

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test39: Tab (U+0009) as the document delimiter beyond the inline and tabular forms already
 * covered by test21 and test22 (§11).
 *
 * - §6/§9.5 – keyed tabular header `gauges[3:<TAB>]{unit<TAB>amount}:`, so the HTAB appears after
 *   the keyed colon inside the bracket segment as well as between field names, and each entry row
 *   is `entrykey: cell<TAB>cell`.
 * - §9.2 – nested arrays where every inner header carries the marker, including the empty inner
 *   array `- [0<TAB>]:`.
 * - §7.2/§11.1 – the quoting flip under tab: commas and pipes are ordinary data and stay unquoted
 *   in both object field values and entry-row cells, while a literal HTAB is quoted regardless - it
 *   is both the active delimiter and a U+0000-U+001F control - and is escaped as `\t` per §7.1,
 *   never emitted raw inside the quotes.
 */
class Test39 : Runner() {
    override val ktoon = Ktoon { delimiter = KtoonConfiguration.Delimiter.TAB }

    override fun run() = doTest(data)
}

@Serializable data class Metric(val unit: String, val amount: Int)

@Serializable
data class TabFormsData(
    /** §11.1: neither the comma nor the pipe is the active delimiter, so no quoting. */
    val summary: String,
    /** §7.1/§7.2: a literal tab is quoted and escaped as `\t`. */
    val spaced: String,
    /** §9.5: keyed tabular, `[3:<TAB>]` header with the same quoting flip in cell position. */
    val gauges: Map<String, Metric>,
    /** §9.2: nested arrays; the empty inner array is `- [0<TAB>]:`. */
    val samples: List<List<Int>>,
)

val data =
    TabFormsData(
        summary = "a, b | c",
        spaced = "left\tright",
        gauges =
            mapOf(
                "cpu" to Metric(unit = "pct, avg", amount = 40),
                "net" to Metric(unit = "up|down", amount = 12),
                "disk" to Metric(unit = "read\twrite", amount = 7),
            ),
        samples = listOf(listOf(1, 2, 3), emptyList(), listOf(4)),
    )
