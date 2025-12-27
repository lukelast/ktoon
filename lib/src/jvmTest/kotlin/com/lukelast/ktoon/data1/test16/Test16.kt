package com.lukelast.ktoon.data1.test16

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

class Test16 : Runner() {
    override fun run() = doTest(Root(listOf(listOf(Person(1)))))
}

@Serializable data class Person(val int: Int)

@Serializable data class Root(val a: List<List<Person>>)
