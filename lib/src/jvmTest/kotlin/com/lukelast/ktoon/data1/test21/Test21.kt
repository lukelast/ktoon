package com.lukelast.ktoon.data1.test21

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test21: Tab delimiter with inline primitive arrays (§11)
 * Tests TAB (U+0009) as active delimiter for inline arrays
 */
class Test21 : Runner() {
    override val ktoon = Ktoon { delimiter = KtoonConfiguration.Delimiter.TAB }
    override fun run() = doTest(data)
}

@Serializable
data class TabDelimiterData(
    val stringArray: List<String>,
    val numberArray: List<Int>,
    val booleanArray: List<Boolean>,
    val mixedNumbers: List<Double>,
    val emptyArray: List<String>
)

val data = TabDelimiterData(
    stringArray = listOf("alpha", "beta", "gamma"),
    numberArray = listOf(1, 2, 3, 4, 5),
    booleanArray = listOf(true, false, true),
    mixedNumbers = listOf(1.5, 2.75, 3.0, 4.25),
    emptyArray = emptyList()
)
