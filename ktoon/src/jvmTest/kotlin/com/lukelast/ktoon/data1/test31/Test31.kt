package com.lukelast.ktoon.data1.test31

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test31: All escape sequences and control characters (§7.1)
 * Tests proper escaping in quoted strings:
 * - Backslash: \\ → \\\\
 * - Quote: \" → \\\"
 * - Newline: U+000A → \\n
 * - Carriage return: U+000D → \\r
 * - Tab: U+0009 → \\t
 * Expected: All control characters properly escaped in output
 */
class Test31 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class EscapeSequencesData(
    val backslash: String,
    val quote: String,
    val newline: String,
    val carriageReturn: String,
    val tab: String,
    val combined: String,
    val multipleNewlines: String,
    val quotedText: String,
    val pathWithBackslashes: String,
    val csvLike: String
)

val data = EscapeSequencesData(
    backslash = "\\",
    quote = "\"",
    newline = "\n",
    carriageReturn = "\r",
    tab = "\t",
    combined = "Line1\nLine2\tTabbed",
    multipleNewlines = "First\nSecond\nThird",
    quotedText = "She said \"Hello\"",
    pathWithBackslashes = "C:\\Users\\Documents\\file.txt",
    csvLike = "Name\tAge\nAlice\t30\nBob\t25"
)
