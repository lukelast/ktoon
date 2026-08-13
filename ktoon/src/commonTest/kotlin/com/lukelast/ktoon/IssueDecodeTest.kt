package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

/** Regression tests for decoding issues reported in `.workflow/issues`. */
class IssueDecodeTest {

    private val strict = Ktoon()
    private val lenient = Ktoon { strictMode = false }

    @Serializable data class OneString(val key: String)

    @Test
    fun `a literal unpaired surrogate is rejected while decoding`() {
        // §7.1: `unescaped-char` excludes U+D800–U+DFFF, and the encoder rejects such strings,
        // so accepting one on decode would produce a value that cannot be encoded again.
        val lone = "\uD800"
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("key: \"a${lone}b\"") }
        assertFailsWith<KtoonException> { lenient.decodeFromString<OneString>("key: a${lone}b") }
        assertFailsWith<KtoonException> { strict.decodeFromString<OneString>("a${lone}b: v") }
    }

    @Test
    fun `a well-formed surrogate pair still decodes`() {
        assertEquals(OneString("a😀b"), strict.decodeFromString("key: a😀b"))
    }
}
