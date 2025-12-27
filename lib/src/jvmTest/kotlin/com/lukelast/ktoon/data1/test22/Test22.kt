package com.lukelast.ktoon.data1.test22

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test22: Tab delimiter with tabular arrays (§11)
 * Tests TAB (U+0009) as active delimiter for tabular format
 * Expected header: users[N\t]{id\tname\tage}:
 */
class Test22 : Runner() {
    override val ktoon = Ktoon { delimiter = KtoonConfiguration.Delimiter.TAB }
    override fun run() = doTest(data)
}

@Serializable
data class User(
    val id: Int,
    val name: String,
    val age: Int
)

@Serializable
data class TabularTabData(
    val users: List<User>
)

val data = TabularTabData(
    users = listOf(
        User(id = 1, name = "Alice", age = 30),
        User(id = 2, name = "Bob", age = 25),
        User(id = 3, name = "Charlie", age = 35),
        User(id = 4, name = "Diana", age = 28)
    )
)
