package com.lukelast.ktoon.demo.encode

import com.lukelast.ktoon.KeyFoldingMode
import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.ToonConfiguration
import com.lukelast.ktoon.demo.*

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
        delimiter = ToonConfiguration.Delimiter.PIPE
        keyFolding = KeyFoldingMode.SAFE
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
