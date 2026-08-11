package com.lukelast.ktoon.data1

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isReadable
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.Test

abstract class Runner {
    @Test
    fun test() {
        run()
    }

    abstract fun run()

    open val ktoon = Ktoon.Default

    protected inline fun <reified T> doTest(data: T, testDecode: Boolean = true) {
        val jsonPath = buildPath("data.json")
        val toonPath = buildPath("data.toon")

        val dataToJsonText = jsonPretty.encodeToString(data)

        // Make sure data and json file are in sync.
        jsonPath.writeText(dataToJsonText)

        if (!toonPath.isReadable()) {
            execToonCli(jsonPath, toonPath)
        }
        val toonFileText = toonPath.readText()

        val dataToToonText = ktoon.encodeToString(data)
        assertEquals(
            toonFileText,
            dataToToonText,
            "ktoon encodeToString checked against the toon file",
        )

        if (testDecode) {
            val toonFileTextToData: T = ktoon.decodeFromString(toonFileText)
            assertEquals(
                data,
                toonFileTextToData,
                "ktoon decodeFromString checked against the data",
            )
        }
        val toonFromJsonText = ktoon.encodeJsonToToon(dataToJsonText)
        assertEquals(
            toonFileText,
            toonFromJsonText,
            "ktoon encodeJsonToToon checked against the toon file",
        )
    }

    fun execToonCli(json: Path, toon: Path) {
        // Pinned to the spec version ktoon targets, so golden regeneration is deterministic.
        val cmd =
            mutableListOf("cmd", "/c", "npx", "@toon-format/cli@4.1.1", json.name, "-o", toon.name)

        if (ktoon.configuration.delimiter != KtoonConfiguration.Delimiter.COMMA) {
            cmd.add("--delimiter")
            cmd.add("\"${ktoon.configuration.delimiter.char}\"")
        }

        val process =
            ProcessBuilder().command(cmd).directory(toon.parent.toFile()).inheritIO().start()
        process.waitFor()
    }

    fun buildPath(fileName: String): Path {
        val basePath = Paths.get("src", "jvmTest", "kotlin")
        val packagePath = Paths.get(this::class.java.`package`.name.replace('.', '/'))
        val fullPath = basePath.resolve(packagePath).resolve(fileName)
        return fullPath
    }
}

val jsonPretty = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    explicitNulls = true
}
