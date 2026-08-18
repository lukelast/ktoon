package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Regression tests for decoding issues reported in `.workflow/issues`. */
class IssueDecodeTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable data class OneString(val key: String)

    @Serializable data class Point(val x: Int, val y: Int)

    @Test
    fun `a root whose shape does not match the target type is rejected`() {
        // The root value gets the same shape check as a nested one, so a class or map deserializer
        // never reads a root array positionally: `[2]: 1,2` is an array, not a Point.
        assertFailsWith<KtoonException> { strict.decodeFromString<Point>("[2]: 1,2") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, Int>>("[4]: a,1,b,2")
        }
        assertFailsWith<KtoonException> { lenient.decodeFromString<Point>("[2]: 1,2") }
    }

    @Test
    fun `a trailing scalar after a root list array is ignored in non-strict mode`() {
        // The list-form sibling of the tabular case below: the last item's object reader threw on
        // the dedented scalar before readRoot's §5 trailing-content check could run, so a root
        // list whose items are objects behaved differently from one with scalar items.
        // Expectations from `npx @toon-format/cli@4.1.1`.
        assertEquals(
            Json.parseToJsonElement("""[{"a":1},{"a":2}]"""),
            lenient.decodeToonToJson("[2]:\n  - a: 1\n  - a: 2\nloose"),
        )
        assertEquals(
            Json.parseToJsonElement("""[{"a":{"b":1}}]"""),
            lenient.decodeToonToJson("[1]:\n  - a:\n      b: 1\nloose"),
        )
        assertFailsWith<KtoonException> {
            strict.decodeToonToJson("[2]:\n  - a: 1\n  - a: 2\nloose")
        }
        // A scalar at the object's own depth is still a structural error in either mode (§5.2).
        assertFailsWith<KtoonException> { lenient.decodeToonToJson("outer:\n  a: 1\n  loose") }
        assertFailsWith<KtoonException> { strict.decodeToonToJson("outer:\n  a: 1\n  loose") }
    }

    @Test
    fun `a trailing scalar after a root tabular array is ignored in non-strict mode`() {
        // §5: once a root array is complete no further line may follow — strict mode errors,
        // non-strict may ignore it. The tabular reader treated any leftover value as a row, so
        // the leniency the inline, `[]`, and keyed roots already had never applied here.
        // Expectations from `npx @toon-format/cli@4.1.1`.
        val expected = Json.parseToJsonElement("""[{"id":1}]""")
        assertEquals(expected, lenient.decodeToonToJson("[1]{id}:\n  1\nloose"))
        assertEquals(expected, lenient.decodeToonToJson("[1]{id}:\n  1\n\nloose"))
        assertEquals(expected, lenient.decodeToonToJson("[1]{id}:\n  1\nextra: 1"))
        assertFailsWith<KtoonException> { strict.decodeToonToJson("[1]{id}:\n  1\nloose") }
        // Not a root form: the leftover belongs to the enclosing object, where a scalar line is a
        // structural error in either mode (§5.2, §14.2).
        assertFailsWith<KtoonException> { lenient.decodeToonToJson("items[1]{id}:\n  1\nloose") }
        // A row that is genuinely inside the scope still reports its own indentation error.
        assertFailsWith<KtoonException> { strict.decodeToonToJson("[1]{id}:\n      1") }
    }

    @Test
    fun `a hyphen outside a list scope is ordinary key text`() {
        // §5.2: the list-item class applies only inside an array in list form; everywhere else the
        // line is classified by the remaining classes, so the hyphen belongs to the key. Every
        // expectation is the strict-mode output of `npx @toon-format/cli@4.1.1`.
        val json = { toon: String -> strict.decodeToonToJson(toon) }
        assertEquals(
            Json.parseToJsonElement("""{"outer":{"- key":1}}"""),
            json("outer:\n  - key: 1"),
        )
        assertEquals(
            Json.parseToJsonElement("""{"outer":{"- a":1,"- b":2}}"""),
            json("outer:\n  - a: 1\n  - b: 2"),
        )
        // A hyphen key opening a nested object, and one carrying an array header.
        assertEquals(
            Json.parseToJsonElement("""{"outer":{"- key":{"a":1}}}"""),
            json("outer:\n  - key:\n    a: 1"),
        )
        assertEquals(
            Json.parseToJsonElement("""{"outer":{"- key":[1,2]}}"""),
            json("outer:\n  - key[2]: 1,2"),
        )
        // Still ordinary key text inside a nested object that sits within a list item.
        assertEquals(
            Json.parseToJsonElement("""{"items":[{"outer":{"- key":1}}]}"""),
            json("items[1]:\n  - outer:\n      - key: 1"),
        )
        // A hyphen line with no colon is a key without one, in both modes.
        assertFailsWith<KtoonException> { json("outer:\n  - foo") }
        assertFailsWith<KtoonException> { json("outer:\n  -") }
        assertFailsWith<KtoonException> { lenient.decodeToonToJson("outer:\n  - foo") }
    }

    @Test
    fun `a hyphen at a list item's field depth still belongs to the list`() {
        // The exception to the rule above: inside an array in list form the hyphen keeps its
        // marker meaning, so a hyphen line at the item object's field depth ends the item rather
        // than becoming a field of it. The CLI rejects this document as over-indented.
        assertFailsWith<KtoonException> {
            strict.decodeToonToJson("items[1]:\n  - a: 1\n    - b: 2")
        }
    }

    @Test
    fun `a literal unpaired surrogate is rejected while decoding`() {
        // §7.1: `unescaped-char` excludes U+D800–U+DFFF, and the encoder rejects such strings,
        // so accepting one on decode would produce a value that cannot be encoded again.
        // Built at runtime: a "\uD800" literal is constant-folded into the compiled JS bundle,
        // where the unencodable lone surrogate is mangled to U+FFFD in transit to the browser.
        val lone = Char(0xD800).toString()
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: \"a${lone}b\"") }
        assertFailsWith<KtoonException> { lenient.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("a${lone}b: v") }
    }

    @Test
    fun `a well-formed surrogate pair still decodes`() {
        assertEquals(OneString("a😀b"), strict.decodeFromString("key: a😀b"))
    }

    @Serializable data class OneValue(val value: String)

    @Test
    fun `a dash-prefixed tabular row is a row and not a list item`() {
        // §5.2: outside a list scope a leading hyphen has no structural meaning.
        val rows = listOf(OneValue("- x"), OneValue("- y"))
        assertEquals(rows, strict.decodeFromString("[2]{value}:\n  - x\n  - y"))
    }

    @Serializable data class TwoInts(val a: Int, val b: Int)

    @Serializable data class BlankItem(val a: Map<String, String> = emptyMap(), val b: Int = 0)

    @Serializable data class BlankItems(val items: List<BlankItem>)

    @Serializable data class NestedTable(val items: List<TableItem>)

    @Serializable data class TableItem(val t: List<TwoInts>)

    @Serializable data class TableRoot(val items: List<TwoInts>)

    @Test
    fun `a blank line inside a started list item span errors in strict mode`() {
        // §12: the span starts at the item line, so a blank between a bare field and the next
        // sibling is inside it even though the item's own content had not begun.
        val input = "items[1]:\n  - a:\n\n    b: 1"
        assertFailsWith<KtoonException> { strict.decodeFromString<BlankItems>(input) }
        assertEquals(
            BlankItems(listOf(BlankItem(b = 1))),
            lenient.decodeFromString<BlankItems>(input),
        )
    }

    @Test
    fun `a blank line before a nested table's first row errors inside a started span`() {
        val input = "items[1]:\n  - t[2]{a,b}:\n\n      1,2\n      3,4"
        assertFailsWith<KtoonException> { strict.decodeFromString<NestedTable>(input) }
    }

    @Test
    fun `a blank line before a root header's first row is still accepted`() {
        // §12: blanks between a header and its scope's first row are ignored, not span errors.
        val input = "items[2]{a,b}:\n\n  1,2\n  3,4"
        val expected = TableRoot(listOf(TwoInts(1, 2), TwoInts(3, 4)))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Serializable data class TableAndField(val items: List<TwoInts>, val other: Int)

    @Test
    fun `a blank line after a scope's content is still accepted`() {
        val input = "items[1]{a,b}:\n  1,2\n\nother: 5"
        assertEquals(
            TableAndField(listOf(TwoInts(1, 2)), 5),
            strict.decodeFromString<TableAndField>(input),
        )
    }

    private fun nestedObjects(levels: Int): String = buildString {
        for (i in 0 until levels) {
            append("  ".repeat(i))
            append("a:\n")
        }
        append("  ".repeat(levels))
        append("b: 1")
    }

    private fun nestedFieldGroups(levels: Int): String =
        "[1]" + "{a".repeat(levels) + "}".repeat(levels) + ":\n  1"

    @Test
    fun `nesting deeper than the configured limit is rejected`() {
        // SPEC §15: a decoder may impose a documented depth limit and report exceeding it.
        val toon = Ktoon { maxNestingDepth = 5 }
        toon.decodeToonToJson(nestedObjects(4))
        assertFailsWith<KtoonException> { toon.decodeToonToJson(nestedObjects(5)) }
    }

    @Test
    fun `very deep values fail with a library error rather than exhausting the stack`() {
        assertFailsWith<KtoonException> { strict.decodeToonToJson(nestedObjects(1000)) }
        assertFailsWith<KtoonException> { lenient.decodeToonToJson(nestedObjects(1000)) }
    }

    @Test
    fun `very deep header field groups fail with a library error`() {
        assertFailsWith<KtoonException> { strict.decodeToonToJson(nestedFieldGroups(1000)) }
        assertFailsWith<KtoonException> { lenient.decodeToonToJson(nestedFieldGroups(1000)) }
    }

    @Test
    fun `a quote inside an unquoted token hides a colon or delimiter`() {
        // Appendix B.3: a quote toggles the quote state wherever it appears, so `a"b: 1` has no
        // unquoted colon and is not a key-value line. §7.4's token-initial rule decides whether an
        // extracted token is unescaped, not where a quoted run starts. `npx
        // @toon-format/cli@4.1.1` rejects both inputs (`Missing colon after key`, and
        // `Expected 2 inline-form values, but got 1`).
        assertFailsWith<KtoonException> { strict.decodeFromString<Map<String, Int>>("a\"b: 1") }
        assertFailsWith<KtoonException> { strict.decodeFromString<List<String>>("[2]: a\"b,c") }
    }

    @Test
    fun `a backslash outside a quoted token does not hide a colon or delimiter`() {
        // §7.1/§7.4: backslash escapes are syntax only inside a quoted token.
        assertEquals(mapOf("path\\" to 1), strict.decodeFromString<Map<String, Int>>("path\\: 1"))
        assertEquals(
            mapOf("items\\" to listOf("left", "right")),
            strict.decodeFromString<Map<String, List<String>>>("items\\[2]: left,right"),
        )
        assertEquals(
            listOf("left\\", "right"),
            strict.decodeFromString<List<String>>("[2]: left\\,right"),
        )
    }

    @Test
    fun `a backslash in a field list does not hide a separator or brace`() {
        assertEquals(
            Json.parseToJsonElement("""[{"left\\": 1, "right": 2}]"""),
            strict.decodeToonToJson("[1]{left\\,right}:\n  1,2"),
        )
        assertEquals(
            Json.parseToJsonElement("""[{"group\\": {"value": 1}}]"""),
            strict.decodeToonToJson("[1]{group\\{value}}:\n  1"),
        )
        // The comma is still a foreign delimiter for a pipe-delimited header (§14.2).
        assertFailsWith<KtoonException> { strict.decodeToonToJson("[1|]{left\\,right}:\n  1|2") }
    }

    @Test
    fun `a token that starts with a quote is still a quoted token`() {
        assertEquals(mapOf("a:b" to 1), strict.decodeFromString<Map<String, Int>>("\"a:b\": 1"))
        assertEquals(listOf("a,b", "c"), strict.decodeFromString<List<String>>("[2]: \"a,b\",c"))
    }

    @Test
    fun `a string value is not converted to a number`() {
        // §4: quoted tokens stay strings, and `05` fails the number grammar; neither is a number.
        for (ktoon in listOf(strict, lenient)) {
            assertFailsWith<KtoonException> { ktoon.decodeFromString<Int>("\"42\"") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<OneInt>("value: 05") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<List<Int>>("[1]: 05") }
            assertEquals(OneInt(5), ktoon.decodeFromString<OneInt>("value: 5"))
        }
    }

    @Test
    fun `a number or boolean does not decode as a string`() {
        // §7.4: a string that looks like a literal is quoted, so an unquoted one is a real
        // number or boolean and the document is mistyped.
        for (ktoon in listOf(strict, lenient)) {
            assertFailsWith<KtoonException> { ktoon.decodeFromString<OneString>("key: 42") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<OneString>("key: false") }
            assertFailsWith<KtoonException> { ktoon.decodeFromString<OneString>("key: null") }
            assertEquals(OneString("42"), ktoon.decodeFromString<OneString>("key: \"42\""))
            assertEquals(OneString("plain"), ktoon.decodeFromString<OneString>("key: plain"))
        }
    }

    @Serializable data class OneLong(val value: Long)

    @Test
    fun `a number outside the requested type's range is rejected`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<OneInt>("value: 2147483648") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneInt>("value: -2147483649") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<OneLong>("value: 9223372036854775808")
        }
        assertEquals(OneInt(2147483647), strict.decodeFromString("value: 2147483647"))
        assertEquals(
            OneLong(9223372036854775807),
            strict.decodeFromString("value: 9223372036854775807"),
        )
    }

    @Test
    fun `a fractional number is not truncated into an integer field`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<OneInt>("value: 1.5") }
        assertEquals(OneInt(15), strict.decodeFromString("value: 1.5e1"))
    }

    @Test
    fun `the two to the sixty-third literal does not become Long MAX_VALUE`() {
        assertEquals(
            Json.parseToJsonElement("""{"value":9223372036854775808}"""),
            strict.decodeToonToJson("value: 9223372036854775808"),
        )
    }

    @Test
    fun `large integer map keys keep every digit`() {
        val input = "\"9007199254740992\": a\n\"9007199254740993\": b"
        assertEquals(
            mapOf(9007199254740992L to "a", 9007199254740993L to "b"),
            strict.decodeFromString<Map<Long, String>>(input),
        )
    }

    @Serializable data class OneBoolean(val value: Boolean)

    @Test
    fun `a string that is not a boolean literal does not decode as false`() {
        for (ktoon in listOf(strict, lenient)) {
            for (token in listOf("yes", "no", "TRUE", "\"maybe\"")) {
                assertFailsWith<KtoonException> {
                    ktoon.decodeFromString<OneBoolean>("value: $token")
                }
            }
            assertEquals(OneBoolean(false), ktoon.decodeFromString<OneBoolean>("value: false"))
        }
    }

    @Test
    fun `boolean map keys still decode`() {
        assertEquals(
            mapOf(true to 1, false to 2),
            strict.decodeFromString<Map<Boolean, Int>>("true: 1\nfalse: 2"),
        )
    }

    @Test
    fun `numeric map keys still decode`() {
        assertEquals(
            mapOf(1 to "one", 2 to "two"),
            strict.decodeFromString<Map<Int, String>>("\"1\": one\n\"2\": two"),
        )
    }

    @Serializable data class OneDouble(val value: Double)

    @Test
    fun `schemaless decoding keeps a number's exact literal`() {
        // §4: lossless-first is recommended; the host Double cannot hold these digits.
        assertEquals(
            Json.parseToJsonElement("""{"value":0.123456789012345678901}"""),
            strict.decodeToonToJson("value: 0.123456789012345678901"),
        )
        assertEquals(
            Json.parseToJsonElement("""{"value":1e21}"""),
            strict.decodeToonToJson("value: 1e21"),
        )
        // Asking for a Double still yields the host approximation.
        assertEquals(
            OneDouble(0.12345678901234568),
            strict.decodeFromString<OneDouble>("value: 0.123456789012345678901"),
        )
    }

    @Test
    fun `a quoted token error does not report a made-up column`() {
        val error = assertFailsWith<KtoonParsingException> { strict.decodeToonToJson("\"abc\\q\"") }
        assertEquals(1, error.line)
        assertEquals(-1, error.column)
        assertEquals(
            "Invalid escape sequence: '\\q' " +
                "(only \\\\, \\\", \\n, \\r, \\t, \\uXXXX are allowed) at line 1",
            error.message,
        )
    }

    @Test
    fun `a lone quote reports an unterminated string`() {
        for (ktoon in listOf(strict, lenient)) {
            for (input in listOf("\"", "a: \"")) {
                val error =
                    assertFailsWith<KtoonParsingException> {
                        ktoon.decodeFromString<Map<String, String>>(input)
                    }
                assertEquals(1, error.line)
                assertEquals("Unterminated string literal at line 1", error.message)
            }
        }
    }

    @Test
    fun `content a root object could not read is not silently dropped`() {
        // §5: a root object extends to the last line, so the non-strict allowance for trailing
        // content after a root array or keyed root object does not apply to it.
        assertFailsWith<KtoonException> {
            lenient.decodeToonToJson("fruits:\n  - apple\n  - banana")
        }
        assertFailsWith<KtoonException> { lenient.decodeToonToJson("a: 1\n\nloose") }
    }

    @Test
    fun `trailing content after a root array is still ignored in non-strict mode`() {
        assertEquals(listOf(1, 2), lenient.decodeFromString<List<Int>>("[2]: 1,2\nextra: 1"))
        assertEquals(emptyList(), lenient.decodeFromString<List<Int>>("[]\nextra: 1"))
        assertEquals(
            mapOf("a" to OneValueInt(1)),
            lenient.decodeFromString<Map<String, OneValueInt>>("[1:]{v}:\n  a: 1\nextra: 1"),
        )
    }

    @Test
    fun `an indented root line is rejected in strict mode`() {
        // §5: root-form discovery works on depth-0 lines.
        assertFailsWith<KtoonException> { strict.decodeFromString<Int>("  42") }
        assertFailsWith<KtoonException> { strict.decodeFromString<List<Int>>("  []") }
        assertFailsWith<KtoonException> { strict.decodeFromString<List<Int>>("  [1]: 7") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, OneValueInt>>("  [1:]{v}:\n    row: 7")
        }
        assertEquals(42, lenient.decodeFromString<Int>("  42"))
        assertEquals(listOf(7), lenient.decodeFromString<List<Int>>("  [1]: 7"))
    }

    @Serializable data class OneValueInt(val v: Int)

    @Test
    fun `an over-indented header-shaped keyed entry is rejected in strict mode`() {
        // §9.5/§12: an entry row stands at exactly the entry depth whatever its key looks like.
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, OneValueInt>>("[1:]{v}:\n    k[1]: 1")
        }
        assertEquals(
            mapOf("k[1]" to OneValueInt(1)),
            strict.decodeFromString<Map<String, OneValueInt>>("[1:]{v}:\n  k[1]: 1"),
        )
        assertEquals(
            mapOf("k[1]" to OneValueInt(1)),
            lenient.decodeFromString<Map<String, OneValueInt>>("[1:]{v}:\n    k[1]: 1"),
        )
    }

    @Serializable data class OneInt(val value: Int)

    @Test
    fun `a dash-prefixed keyed entry row keeps the hyphen in its entry key`() {
        val expected = mapOf("- key" to OneInt(1), "other" to OneInt(2))
        assertEquals(expected, strict.decodeFromString("[2:]{value}:\n  - key: 1\n  other: 2"))
    }
}
