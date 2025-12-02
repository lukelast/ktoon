package com.lukelast.ktoon.demo.encode

import com.lukelast.ktoon.Ktoon

/** Convert raw JSON text straight to TOON text. */
fun main() {
    val jsonText =
        """
        {
            "name": "Tech Corp",
            "employees": [
                {
                    "id": 1,
                    "name": "Alice",
                    "role": "admin"
                },
                {
                    "id": 2,
                    "name": "Bob",
                    "role": "user"
                },
                {
                    "id": 3,
                    "name": "Charlie",
                    "role": "user"
                },
                {
                    "id": 4,
                    "name": "Dana",
                    "role": "user"
                },
                {
                    "id": 5,
                    "name": "Eve",
                    "role": "guest"
                }
            ]
        }
        """
            .trimIndent()

    val toonText = Ktoon.Default.encodeJsonToToon(jsonText)

    println("##### JSON format:")
    println(jsonText)
    println()
    println("##### TOON format:")
    println(toonText)
}
