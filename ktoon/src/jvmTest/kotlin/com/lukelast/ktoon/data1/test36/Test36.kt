package com.lukelast.ktoon.data1.test36

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Test36: Key encoding (§7.3) driven by real map keys
 *
 * test26 exercises key quoting through `@SerialName` only, and test20 only uses integer map keys.
 * This test drives §7.3 from actual `Map<String, Int>` keys, sweeping the boundary of the
 * unquoted-key pattern `^[A-Za-z_][A-Za-z0-9_.]*$`, and then checks that a key needing quotes is
 * still quoted when it moves into an array header ("MUST be quoted in all contexts").
 *
 * Expected:
 * - bare: `true` (the boolean/null value rule of §7.2 does not apply to keys), `__proto__`, `_a.b9`
 * - quoted: "" (empty), "42" (leading digit), "2key" (leading digit), "a b" (space), "a:b" (colon),
 *   "#" (not in the pattern), "名前" (non-ASCII), plus "a\"b" / "a\nb" which are quoted and escaped
 *   per §7.1
 * - array headers carry the quotes: `"my-list"[3]:` and `"row-data"[2]{"field name",ok}:`
 */
class Test36 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class KeyFormattingData(
    val keys: Map<String, Int>,

    // A quoted key on an inline primitive array -> "my-list"[3]: 1,2,3
    @SerialName("my-list") val myList: List<Int>,

    // A quoted key on a tabular array, with a quoted field name inside the header ->
    // "row-data"[2]{"field name",ok}:
    @SerialName("row-data") val rowData: List<RowData>,
)

@Serializable
data class RowData(
    @SerialName("field name") val fieldName: String,
    val ok: Boolean,
)

val data =
    KeyFormattingData(
        keys =
            linkedMapOf(
                // "42" is declared first on purpose: the reference CLI reads the JSON into a
                // JavaScript object, where array-index-like keys are always enumerated before
                // string keys. Declaring it first keeps ktoon's insertion order and the golden
                // in agreement.
                "42" to 6, // quoted: leading digit

                // Quoted: does not match ^[A-Za-z_][A-Za-z0-9_.]*$
                "" to 1, // empty key
                "2key" to 2, // leading digit
                "a b" to 3, // space
                "a:b" to 4, // colon

                // Bare: "true" is a legal unquoted key even though the string value "true"
                // would have to be quoted (§7.2 vs §7.3).
                "true" to 5,

                // Quoted: "#" is not in the unquoted-key pattern.
                "#" to 7,

                // Bare: both match the unquoted-key pattern (leading underscore, dots and
                // digits after the first character are allowed).
                "__proto__" to 8,
                "_a.b9" to 9,

                // Quoted: non-ASCII is outside the ASCII-only unquoted-key pattern.
                "名前" to 10,

                // Quoted and escaped per §7.1.
                "a\"b" to 11,
                "a\nb" to 12,
            ),
        myList = listOf(1, 2, 3),
        rowData =
            listOf(
                RowData(fieldName = "alpha", ok = true),
                RowData(fieldName = "beta", ok = false),
            ),
    )
