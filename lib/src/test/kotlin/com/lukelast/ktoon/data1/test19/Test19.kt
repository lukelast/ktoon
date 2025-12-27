package com.lukelast.ktoon.data1.test19

import com.lukelast.ktoon.data1.Runner
import kotlin.test.Ignore

@Ignore("There is a bug in the official TOON cli")
class Test19 : Runner() {
    override fun run() = doTest(mapOf(2 to listOf("two"), 1 to listOf("one")))
}
