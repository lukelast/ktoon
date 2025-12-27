package com.lukelast.ktoon.data1.test11

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/** https://github.com/toon-format/spec/blob/main/examples/conversions/users.toon */
class Test11 : Runner() {
    override fun run() = doTest(data)
}

val data =
    UsersResponse(
        users =
            listOf(
                User(id = 1, name = "Alice", role = "admin", active = true),
                User(id = 2, name = "Bob", role = "developer", active = true),
                User(id = 3, name = "Charlie", role = "designer", active = false),
            )
    )

@Serializable data class UsersResponse(val users: List<User>)

@Serializable data class User(val id: Int, val name: String, val role: String, val active: Boolean)
