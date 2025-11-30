package com.lukelast.ktoon.demo.encode

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.demo.Company
import com.lukelast.ktoon.demo.User
import com.lukelast.ktoon.demo.json

fun main() {
    val company =
        Company(
            name = "Tech Corp",
            employees =
                listOf(
                    User(1, "Alice", "admin"),
                    User(2, "Bob", "user"),
                    User(3, "Charlie", "user"),
                    User(4, "Dana", "user"),
                    User(5, "Eve", "guest"),
                ),
        )

    val ktoon = Ktoon.Default
    val toonText = ktoon.encodeToString(company)
    val jsonText = json.encodeToString(company)

    println("##### JSON format:")
    println(jsonText)
    println()
    println("##### TOON format:")
    println(toonText)
}
