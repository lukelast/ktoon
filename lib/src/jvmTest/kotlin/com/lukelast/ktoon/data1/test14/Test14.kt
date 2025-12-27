package com.lukelast.ktoon.data1.test14

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

class Test14 : Runner() {
    override val ktoon = Ktoon { delimiter = KtoonConfiguration.Delimiter.PIPE }

    override fun run() = doTest(Root(listOf("one", "two", "three")))
}

@Serializable data class Root(val items: List<String>)
