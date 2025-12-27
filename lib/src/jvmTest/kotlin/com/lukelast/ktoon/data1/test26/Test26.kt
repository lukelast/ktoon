package com.lukelast.ktoon.data1.test26

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Test26: Keys with hyphens, spaces, and special characters (§7.3)
 * Tests keys requiring quoting per spec:
 * - Keys with hyphens (not matching ^[A-Za-z_][A-Za-z0-9_.]*)
 * - Keys with spaces
 * - Keys with colons
 * - Keys with brackets
 * - Keys with quotes
 * Expected: All keys quoted and escaped appropriately
 */
class Test26 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class SpecialKeysData(
    @SerialName("my-key")
    val myKey: String,

    @SerialName("full name")
    val fullName: String,

    @SerialName("key:with:colons")
    val keyWithColons: String,

    @SerialName("key[with]brackets")
    val keyWithBrackets: String,

    @SerialName("key\"with\"quotes")
    val keyWithQuotes: String,

    @SerialName("key{with}braces")
    val keyWithBraces: String,

    @SerialName("key\\with\\backslashes")
    val keyWithBackslashes: String,

    @SerialName("tab\ttab")
    val keyWithTab: String,

    @SerialName("newline\nkey")
    val keyWithNewline: String
)

val data = SpecialKeysData(
    myKey = "value1",
    fullName = "Ada Lovelace",
    keyWithColons = "value2",
    keyWithBrackets = "value3",
    keyWithQuotes = "value4",
    keyWithBraces = "value5",
    keyWithBackslashes = "value6",
    keyWithTab = "value7",
    keyWithNewline = "value8"
)
