package com.lukelast.ktoon.data1.test15

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

class Test15 : AbstractGoldenTest() {
    override fun verify() = assertGolden(Root(listOf(listOf(Person(1u)))))
}

@Serializable data class Person(val uint: UInt)

@Serializable data class Root(val a: List<List<Person>>)
