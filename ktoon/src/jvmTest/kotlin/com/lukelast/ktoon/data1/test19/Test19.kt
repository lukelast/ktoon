package com.lukelast.ktoon.data1.test19

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlin.test.Ignore

@Ignore("There is a bug in the official TOON cli")
class Test19 : AbstractGoldenTest() {
    override fun verify() = assertGolden(mapOf(2 to listOf("two"), 1 to listOf("one")))
}
