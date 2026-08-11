package com.lukelast.ktoon.data1.test42

import com.lukelast.ktoon.data1.Runner

/**
 * Test42: Root inline primitive array (§5 root form, §9.1)
 * The whole document is a single header line with the key omitted: `[N<delim?>]: v1,v2,…`
 * (test04 covers the root *tabular* form; this covers the root *inline* form).
 * Quoting is delimiter-aware and numeric-lookalike aware (§7.2):
 * - "x,y" contains the active delimiter, so it MUST be quoted
 * - "42" would decode as a number unquoted, so it MUST be quoted
 * Expected document: `[4]: a,"x,y",c,"42"` on one line, no trailing newline
 */
class Test42 : Runner() {
    override fun run() = doTest(data)
}

val data = listOf("a", "x,y", "c", "42")
