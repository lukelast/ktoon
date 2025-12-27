package com.lukelast.ktoon.data1.test05

import com.lukelast.ktoon.data1.Garage
import com.lukelast.ktoon.data1.Runner

class Test05 : Runner() {
    override fun run() = doTest(data)
}

private val data = Garage(owner = "", location = "", capacity = 0, inventory = listOf())
