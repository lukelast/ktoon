package com.lukelast.ktoon.demo

import com.lukelast.ktoon.Ktoon
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val sampleData = Company(
    name = "Tech Corp",
    employees = listOf(
        User(1, "Alice", "admin"),
        User(2, "Bob", "user"),
        User(3, "Charlie", "user"),
        User(4, "Dana", "user"),
        User(5, "Eve", "guest"),
    ),
)

private val json = Json { prettyPrint = true }

fun runDemo(): String {
    val ktoon = Ktoon.Default

    val toonText = ktoon.encodeToString(sampleData)
    val jsonText = json.encodeToString(sampleData)

    // Verify round-trip works
    val decoded: Company = ktoon.decodeFromString(toonText)
    val roundTripOk = decoded == sampleData

    return buildString {
        appendLine("===== JSON format =====")
        appendLine(jsonText)
        appendLine()
        appendLine("===== TOON format =====")
        appendLine(toonText)
        appendLine()
        appendLine("===== Round-trip test =====")
        appendLine("Decoded matches original: $roundTripOk")
    }
}
