package com.lukelast.ktoon.data1.test12

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/** Key folding feature test. Tests safe key folding for nested objects. */
class Test12 : Runner() {
    override val ktoon = Ktoon { keyFoldingSafe() }

    override fun run() = doTest(data)
}

@Serializable data class Root(val blah: String, val one: Field1)

@Serializable data class Field1(val two: Field2)

@Serializable data class Field2(val three: Int)

private val data = Root("foo", Field1(Field2(123)))
