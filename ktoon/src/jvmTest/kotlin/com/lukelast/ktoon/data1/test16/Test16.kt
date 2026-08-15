package com.lukelast.ktoon.data1.test16

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

class Test16 : AbstractGoldenTest() {
    override fun verify() = assertGolden(Root(listOf(listOf(Person(1)))))
}

@Serializable data class Person(val int: Int)

@Serializable data class Root(val a: List<List<Person>>)
