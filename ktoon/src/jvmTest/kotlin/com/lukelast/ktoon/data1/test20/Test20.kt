package com.lukelast.ktoon.data1.test20

import com.lukelast.ktoon.data1.AbstractGoldenTest

class Test20 : AbstractGoldenTest() {
    override fun verify() = assertGolden(mapOf(1 to listOf("one"), 2 to listOf("two")))
}
