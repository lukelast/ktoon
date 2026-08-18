package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Regression tests for lexer/header issues reported in `.workflow/issues`. */
class IssueLexerTest {

    private val strict = Ktoon()

    @Serializable data class Named(val id: Int, val name: String)

    @Serializable data class GroupHolder(val group: Named)

    @Serializable data class GroupRoot(val items: List<GroupHolder>)

    private val lenient = Ktoon { strictMode = false }

    @Test
    fun `a declared length too large for the host is still a header`() {
        // §6 puts no bound on the declared length; only the count check cares about its value.
        assertEquals(
            mapOf("items" to listOf("x")),
            lenient.decodeFromString<Map<String, List<String>>>("items[2147483648]: x"),
        )
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, List<String>>>("items[2147483648]: x")
        }
    }

    @Test
    fun `a count mismatch reports the declared length as written`() {
        // The parsed length saturates beyond Long, so the message must quote the document's own
        // literal, not the saturated host value.
        val literal = "99999999999999999999"
        val error =
            assertFailsWith<KtoonException> {
                strict.decodeFromString<Map<String, List<String>>>("items[$literal]: x")
            }
        assertEquals("Array length mismatch: declared $literal, found 1 at line 1", error.message)
    }

    @Serializable data class TwoStrings(val a: String, val b: String)

    @Serializable data class IntAndString(val a: Int, val b: String)

    @Test
    fun `a row whose cell looks like a malformed header is still a row`() {
        // §9.3: at row depth the delimiter-before-colon rule decides, not the header grammar.
        val input = "[2]{a,b}:\n  1,Ada\n  2,foo [2]: x"
        val expected = listOf(IntAndString(1, "Ada"), IntAndString(2, "foo [2]: x"))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Test
    fun `an entry key that looks like a malformed header is still an entry row`() {
        // §9.5: every line at entry depth with an unquoted colon is an entry row.
        val input = "[1:]{a,b}:\n  foo[]: 1,two"
        assertEquals(
            mapOf("foo[]" to IntAndString(1, "two")),
            strict.decodeFromString<Map<String, IntAndString>>(input),
        )
    }

    @Test
    fun `a malformed header in a key position is still an error`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<TwoStrings>("foo [2]: 1,2") }
        assertFailsWith<KtoonException> { strict.decodeFromString<TwoStrings>("key[]: 1") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, String>>("outer:\n  key[03]: 1")
        }
    }

    @Test
    fun `a tab-delimited row keeps an empty leading cell`() {
        // §11.2: splitting preserves empty tokens, so the row's leading tab is the delimiter and
        // not indentation.
        val input = "[2\t]{a\tb}:\n  \tone\n  two\tthree"
        val expected = listOf(TwoStrings("", "one"), TwoStrings("two", "three"))
        assertEquals(expected, strict.decodeFromString(input))
    }

    @Test
    fun `a tab-delimited row of only empty cells is a row and not a blank line`() {
        val input = "[1\t]{a\tb}:\n  \t"
        assertEquals(listOf(TwoStrings("", "")), strict.decodeFromString(input))
    }

    @Test
    fun `a whitespace-only line containing a tab is not blank in strict mode`() {
        // §12: trimming strips only U+0020, so such a line does not trim to empty — it is a
        // tab-indented line, which strict mode must reject. `npx @toon-format/cli@4.1.1` reports
        // "Tabs are not allowed in indentation in strict mode" for every spelling below and
        // decodes them as blank with --no-strict. The space-first spelling ` \t` reaches the same
        // verdict through the row branch above, which keeps it available as a row of empty cells.
        for (blank in listOf("\t", "\t\t", "\t ", " \t")) {
            assertFailsWith<KtoonException>("strict accepted \"$blank\"") {
                strict.decodeFromString<TwoInts>("a: 1\n$blank\nb: 2")
            }
            assertEquals(
                TwoInts(1, 2),
                lenient.decodeFromString<TwoInts>("a: 1\n$blank\nb: 2"),
                "non-strict rejected \"$blank\"",
            )
        }
    }

    @Serializable data class TwoInts(val a: Int, val b: Int)

    @Test
    fun `a tab used as indentation is still an error`() {
        assertFailsWith<KtoonException> { strict.decodeFromString<TwoStrings>("\ta: 1\nb: 2") }
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, String>>("a:\n  \tb: 1")
        }
    }

    @Test
    fun `a space before a nested field group's brace is not part of the field name`() {
        // §12: field names are extracted tokens, so surrounding spaces are trimmed. Verified
        // against `npx @toon-format/cli@4.1.1`, which accepts this in strict mode and decodes
        // the column as `group`.
        val input = "items[1]{group {id,name}}:\n  1,Ada"
        assertEquals(
            GroupRoot(listOf(GroupHolder(Named(1, "Ada")))),
            strict.decodeFromString(input),
        )
    }

    @Test
    fun `a quote inside an unquoted cell hides the delimiters that follow it`() {
        // SPEC Appendix B.3 `parseDelimitedValues` toggles the quote state on every `"`, so the
        // comma in `a"b,c"` is quoted and the line carries one value, not two. Every expectation
        // below is the output of `npx @toon-format/cli@4.1.1`.
        assertEquals(
            mapOf("items" to listOf("a\"b,c\"")),
            strict.decodeFromString<Map<String, List<String>>>("items[1]: a\"b,c\""),
        )
        // The CLI reports `Expected 2 inline-form values, but got 1`.
        assertFailsWith<KtoonException> {
            strict.decodeFromString<Map<String, List<String>>>("items[2]: a\"b,c\"")
        }
        // ... and `Expected 2 tabular row values, but got 1` for the same cell in a row.
        assertFailsWith<KtoonException> { strict.decodeToonToJson("items[1]{x,y}:\n  a\"b,c\"") }
        // An unbalanced quote swallows the rest of the line the same way.
        assertEquals(listOf("a\"b,c"), strict.decodeFromString<List<String>>("[1]: a\"b,c"))
        assertFailsWith<KtoonException> { strict.decodeFromString<List<String>>("[2]: a\"b,c") }
    }

    @Test
    fun `a quote inside a field name leaves the field list unterminated`() {
        // Appendix B.3 applies to the field list too: `a"b,c` never closes its quote, so the
        // header is malformed. The CLI rejects `items[1]{a"b,c}:` in strict mode.
        assertFailsWith<KtoonException> { strict.decodeToonToJson("items[1]{a\"b,c}:\n  1,2") }
    }

    @Test
    fun `a quote after a non-active delimiter still opens a quoted token`() {
        // A quote is token-initial after any delimiter character, not only the active one, so the
        // colon in `x|"a:1",2` is quoted and the line is a row. Expected JSON is the strict-mode
        // output of `npx @toon-format/cli@4.1.1`.
        val input = "[2]{a,b}:\n  1,Ada\n  x|\"a:1\",2"
        val expected = """[{"a":1,"b":"Ada"},{"a":"x|\"a:1\"","b":2}]"""
        assertEquals(Json.parseToJsonElement(expected), strict.decodeToonToJson(input))
    }
}
