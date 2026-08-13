package com.lukelast.ktoon.demo.encode

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.demo.Ceo
import com.lukelast.ktoon.demo.Company
import com.lukelast.ktoon.demo.ParentCompany
import com.lukelast.ktoon.demo.User
import com.lukelast.ktoon.demo.json

fun main() {
    val data =
        ParentCompany(
            name = "Conglomerate",
            leader = Ceo("Kristen"),
            organizations =
                listOf(
                    Company(
                        name = "Subsidiary",
                        employees =
                            listOf(
                                User(1, "Alice", "admin"),
                                User(2, "Bob", "user"),
                                User(3, "Charlie", "user"),
                                User(4, "Dana", "user"),
                                User(5, "Eve", "guest"),
                            ),
                    ),
                    Company(
                        name = "Acquisition",
                        employees =
                            listOf(
                                User(6, "Frank", "admin"),
                                User(7, "Grace", "user"),
                                User(8, "Hank", "user"),
                            ),
                    ),
                ),
        )

    val ktoon = Ktoon {
        delimiter = KtoonConfiguration.Delimiter.PIPE
        indentSize = 1
    }
    val toonText = ktoon.encodeToString(data)
    val jsonText = json.encodeToString(data)

    println("##### JSON format:")
    println(jsonText)
    println()
    println("##### TOON format:")
    println(toonText)
}
