package com.lukelast.ktoon.data1.test13

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

class Test12 : Runner() {
    override val ktoon = Ktoon { keyFoldingSafe() }

    override fun run() = doTest(Root(Field1(Field2(listOf("a", "b")))))
}

@Serializable data class Root(val one: Field1)

@Serializable data class Field1(val two: Field2)

@Serializable data class Field2(val three: List<String>)
