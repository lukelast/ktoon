package com.lukelast.ktoon.data1.test35

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test35: Remaining §7.2 string-value quoting triggers (and their near-misses)
 *
 * Complements test09 (quotes/colons/commas/brackets, "null"/"true"/"911"/""/leading hyphen) and
 * test31 (the five named escapes) by covering the triggers those tests leave untested, plus the
 * look-alike strings that MUST stay bare so the trigger set is not over-applied:
 * - leading `#` (§7.2 number-sign rule)
 * - numeric-like per `/^[+-]?[0-9]+(?:\.[0-9]+)?(?:e[+-]?[0-9]+)?$/i`: "+1", "05", "1e5", "1E5"
 * - leading/trailing whitespace, including a leading HTAB that also escapes to `\t` (§7.1)
 * - braces `{` and `}`
 * - near-misses that MUST stay bare: ".5", "1.", "1_000", "0x10", "NaN", "Infinity", "TRUE",
 *   "x-y", "a@b.c!?", "+abc"
 * - tabular row cells carrying an escaped multi-line string next to a bare emoji cell
 *
 * Expected: exactly the "quote-required" group is quoted; the "stays bare" group is emitted
 * unquoted; "-Infinity" is quoted only because of its position-0 hyphen, not because it is
 * numeric-like.
 */
class Test35 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class QuotingTriggersData(
    // Quote-required: number sign at position 0.
    val leadingHash: String,

    // Quote-required: numeric-like. "+1" (sign), "05" (leading zero), "1e5"/"1E5" (the exponent
    // part of the pattern is case-insensitive, so both spellings are numeric-like).
    val plusNumeric: String,
    val leadingZeroNumeric: String,
    val expLower: String,
    val expUpper: String,

    // Quote-required: the bare hyphen would otherwise read as a list-item marker.
    val hyphenAlone: String,

    // Quote-required: leading / trailing whitespace (U+0020), and a leading HTAB which is both a
    // whitespace trigger and a control character escaped as \t.
    val leadingSpace: String,
    val trailingSpace: String,
    val leadingTab: String,

    // Quote-required: braces anywhere in the value.
    val openBrace: String,
    val closeBrace: String,

    // Stays bare: ".5" and "1." are NOT numeric-like - the pattern requires digits on both sides
    // of the decimal point.
    val dotFive: String,
    val trailingDot: String,

    // Stays bare: digit separators and hex literals are not part of the numeric-like pattern.
    val underscoreDigits: String,
    val hexLike: String,

    // Stays bare: NaN / Infinity are not TOON numbers, and the boolean/null test is
    // case-sensitive so "TRUE" is an ordinary string.
    val nanWord: String,
    val infinityWord: String,
    val trueUpper: String,

    // Quoted, but only because of the position-0 hyphen rule - "Infinity" alone stays bare.
    val negInfinityWord: String,

    // Stays bare: a hyphen anywhere other than position 0 is not a trigger.
    val innerHyphen: String,

    // Stays bare: `@ . ! ?` and a non-numeric-like `+` prefix are not triggers.
    val punctuation: String,
    val plusWord: String,

    // Tabular rows: the multi-line cell is quoted and escaped, the emoji cell stays bare even
    // with an internal space.
    val rows: List<Row>,
)

@Serializable
data class Row(
    val id: Int,
    val note: String,
    val tag: String,
)

val data =
    QuotingTriggersData(
        leadingHash = "#tag",
        plusNumeric = "+1",
        leadingZeroNumeric = "05",
        expLower = "1e5",
        expUpper = "1E5",
        hyphenAlone = "-",
        leadingSpace = " lead",
        trailingSpace = "trail ",
        leadingTab = "\tx",
        openBrace = "a{b",
        closeBrace = "a}b",
        dotFive = ".5",
        trailingDot = "1.",
        underscoreDigits = "1_000",
        hexLike = "0x10",
        nanWord = "NaN",
        infinityWord = "Infinity",
        trueUpper = "TRUE",
        negInfinityWord = "-Infinity",
        innerHyphen = "x-y",
        punctuation = "a@b.c!?",
        plusWord = "+abc",
        rows =
            listOf(
                Row(id = 1, note = "line1\nline2", tag = "🚀 launch"),
                Row(id = 2, note = "plain note", tag = "✅"),
            ),
    )
