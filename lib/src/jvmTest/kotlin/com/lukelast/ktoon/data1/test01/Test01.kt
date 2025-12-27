package com.lukelast.ktoon.data1.test01

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/** Example from https://toontools.vercel.app/tools/json-to-toon "Simple Example". */
class Test01 : Runner() {
    override fun run() = doTest(data)
}

@Serializable data class UserList(val users: List<User>)

@Serializable
data class User(
    val id: Int,
    val name: String,
    val role: String, // See note below about using Enums here
)

val data =
    UserList(
        users =
            listOf(
                User(id = 1, name = "Alice", role = "admin"),
                User(id = 2, name = "Bob", role = "user"),
            )
    )
