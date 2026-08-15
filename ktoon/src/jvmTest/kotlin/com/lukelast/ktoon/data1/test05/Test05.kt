package com.lukelast.ktoon.data1.test05

import com.lukelast.ktoon.data1.AbstractGoldenTest
import com.lukelast.ktoon.data1.Garage

class Test05 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

private val data = Garage(owner = "", location = "", capacity = 0, inventory = listOf())
