package com.lukelast.ktoon.data1.test37

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test37: Control characters and Unicode (§7.1 escaping, §7.2 value quoting, §7.3 key encoding, §16
 * internationalization).
 *
 * The five short escapes (backslash, double quote, LF, CR, HTAB) are already covered by Test31;
 * this test covers everything else about control characters and non-ASCII text.
 *
 * Behaviour verified byte-for-byte against `npx @toon-format/cli@4.1.1`:
 * - §7.1 row 6: C0 controls other than LF/CR/HTAB are emitted as `\uXXXX` with **lowercase** hex
 *   and always four digits. Containing one also trips §7.2 ("contains control characters in U+0000
 *   through U+001F"), so those values are quoted.
 * - §7.2: DEL (U+007F) is *not* in U+0000-U+001F, so it is neither escaped nor a reason to quote:
 *   the golden holds a bare 0x7F byte inside an unquoted value.
 * - §7.2: only U+0020 and U+0009 count as whitespace, so a leading or trailing NBSP (U+00A0) does
 *   not force quoting. The golden holds a bare C2 A0 at the edge of an unquoted value.
 * - §7.1 row 7: U+2028 LINE SEPARATOR is an ordinary BMP codepoint. It is emitted literally (E2 80
 *   A8), not escaped, and it does not terminate a line (§12 splits lines on LF only).
 * - §7.1 row 8 / §16: supplementary codepoints are emitted as literal UTF-8, never as surrogate
 *   escapes; combining marks, ZWJ sequences and regional-indicator pairs pass through unchanged.
 * - §7.3 / §16: the bare-key pattern is ASCII-only, so non-ASCII map keys are always quoted even
 *   though the very same text is safe unquoted as a *value*.
 */
class Test37 : Runner() {
    override fun run() = doTest(data)
}

@Serializable data class UnicodeRow(val label: String, val emoji: String, val count: Int)

@Serializable
data class UnicodeData(
    val nulEmbedded: String,
    val bellEmbedded: String,
    val unitSepEmbedded: String,
    val delEmbedded: String,
    val nbspLeading: String,
    val nbspTrailing: String,
    val lineSeparator: String,
    val japanese: String,
    val arabic: String,
    val hebrew: String,
    val korean: String,
    val combiningMark: String,
    val zwjFamily: String,
    val flag: String,
    val supplementary: String,
    val moods: List<String>,
    val rows: List<UnicodeRow>,
    val counts: Map<String, Int>,
)

val data =
    UnicodeData(
        // §7.1: U+0000 NUL has no short escape, so it is escaped as \u0000 and the value is
        // therefore quoted (§7.2). Expected line: nulEmbedded: "a\u0000b"
        nulEmbedded = "a\u0000b",

        // §7.1: U+0007 BEL -> escaped, quoted. Expected line: bellEmbedded: "a\u0007b"
        bellEmbedded = "a\u0007b",

        // §7.1: U+001F UNIT SEPARATOR is the last C0 control; note the CLI emits lowercase hex.
        // Expected line: unitSepEmbedded: "a\u001fb"
        unitSepEmbedded = "a\u001fb",

        // §7.2: U+007F DEL sits outside U+0000-U+001F, so it is not a control for TOON's purposes.
        // Expected: literal 0x7F byte in a bare (unquoted) value -> delEmbedded: a<DEL>b
        delEmbedded = "a\u007fb",

        // §7.2: "whitespace" is U+0020 or U+0009 only, so a leading NBSP is not leading whitespace.
        // Expected: literal C2 A0 in a bare (unquoted) value -> nbspLeading: <NBSP>x
        nbspLeading = "\u00a0x",

        // Same rule at the trailing edge: the golden line ends with a bare C2 A0 before its LF.
        // Expected: nbspTrailing: x<NBSP>
        nbspTrailing = "x\u00a0",

        // §7.1 row 7: U+2028 LINE SEPARATOR is an ordinary BMP codepoint, emitted as literal UTF-8
        // (E2 80 A8) rather than an escape, and it is not a line terminator (§12).
        // Expected: lineSeparator: a<U+2028>b -- bare, and still a single physical line.
        lineSeparator = "a\u2028b",

        // §16: CJK text is safe unquoted. Expected: japanese: 日本語テキスト
        japanese = "日本語テキスト",

        // §16: RTL Arabic is safe unquoted; no bidi controls are added. Expected: arabic: مرحبا
        arabic = "مرحبا",

        // §16: RTL Hebrew is safe unquoted. Expected: hebrew: שלום
        hebrew = "שלום",

        // §16: Hangul is safe unquoted. Expected: korean: 안녕하세요
        korean = "안녕하세요",

        // §2/§16: "cafe" + U+0301 COMBINING ACUTE stays decomposed; encoders must not normalize.
        // Renders as "cafe" with an accent, but is 5 codepoints. Expected: bare, unchanged.
        combiningMark = "cafe\u0301",

        // §7.1: ZWJ (U+200D) is not a control character, so the whole grapheme cluster is literal
        // UTF-8. Expected: zwjFamily: 👨<ZWJ>👩<ZWJ>👧 -- bare, unquoted.
        zwjFamily = "👨\u200d👩\u200d👧",

        // §16: regional-indicator pair U+1F1EF U+1F1F5. Expected: flag: 🇯🇵 -- bare, unquoted.
        flag = "🇯🇵",

        // §7.1 row 8: supplementary scalar U+1D11E MUST be literal UTF-8, never surrogate escapes.
        // Expected: supplementary: 𝄞 -- bare, unquoted.
        supplementary = "𝄞",

        // §7.2: emoji are safe unquoted inside an inline primitive array.
        // Expected: moods[3]: 🎉,🎊,🎈
        moods = listOf("🎉", "🎊", "🎈"),

        // §6/§16: tabular rows with non-ASCII cells; the ASCII field names stay bare.
        // Expected header: rows[2]{label,emoji,count}:
        // Expected rows:   日本語,🎉,1   and   안녕하세요,🎊,2   (all cells bare)
        rows = listOf(UnicodeRow("日本語", "🎉", 1), UnicodeRow("안녕하세요", "🎊", 2)),

        // §7.3/§16: the bare-key pattern ^[A-Za-z_][A-Za-z0-9_.]*$ is ASCII-only, so both keys are
        // quoted even though the identical text is emitted bare as a value above.
        // Expected: counts: then the entries 日本語 and 🎉 with their keys wrapped in quotes.
        counts = mapOf("日本語" to 3, "🎉" to 7),
    )
