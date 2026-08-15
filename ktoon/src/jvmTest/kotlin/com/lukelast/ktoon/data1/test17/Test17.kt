package com.lukelast.ktoon.data1.test17

import com.lukelast.ktoon.data1.AbstractGoldenTest

class Test17 : AbstractGoldenTest() {
    override fun verify() = assertGolden(mapOf("one" to 1, "two" to 2))
}
