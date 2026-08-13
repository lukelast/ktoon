package com.lukelast.ktoon.decoding

import com.lukelast.ktoon.KtoonConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

/** Regression test for the blank-run scanning issue reported in `.workflow/issues`. */
class ToonParserBlankRunTest {

    /** A token list that counts indexed reads, so scanning growth can be measured. */
    private class CountingTokens(private val delegate: List<Token>) : AbstractList<Token>() {
        var reads = 0
            private set

        override val size: Int
            get() = delegate.size

        override fun get(index: Int): Token {
            reads++
            return delegate[index]
        }
    }

    private fun tokenReadsForBlanks(blanks: Int): Int {
        val input = "a: 1" + "\n".repeat(blanks + 1) + "b: 2"
        val tokens = CountingTokens(ToonLexer(input, KtoonConfiguration.Default).tokenize())
        ToonParser(tokens, KtoonConfiguration.Default).readRoot()
        return tokens.reads
    }

    @Test
    fun `a blank run between fields is scanned once, not once per blank`() {
        // Looking ahead over the whole run and then consuming a single blank made the work grow
        // with the square of the run's length.
        val small = tokenReadsForBlanks(500)
        val large = tokenReadsForBlanks(1_000)
        assertTrue(
            large < small * 3,
            "twice the blanks should not cost far more than twice the reads: $small then $large",
        )
    }
}
