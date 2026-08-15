package com.lukelast.ktoon.data1

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.isReadable
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

/**
 * Base class for golden-file tests that check ktoon against the reference TOON CLI.
 *
 * Each subclass lives in its own package next to two generated files: `data.json`, rewritten on
 * every run from the subclass's data object, and `data.toon`, the golden produced by the pinned
 * `@toon-format/cli` when the file is absent. [assertGolden] asserts that the typed encode, the
 * typed decode round-trip, and the JSON-path encode all agree with the golden. To regenerate a
 * golden after changing the data object, delete its `data.toon` and rerun the test (requires npm).
 */
abstract class AbstractGoldenTest {
    @Test fun test() = verify()

    abstract fun verify()

    open val ktoon = Ktoon.Default

    protected inline fun <reified T> assertGolden(data: T, testDecode: Boolean = true) {
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
            "ktoon encodeToString checked against the toon file. If the data object changed " +
                "intentionally, delete data.toon and rerun to regenerate the golden.",
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
        // Windows needs the cmd shell to resolve npx.cmd, and shell quoting for the delimiter;
        // elsewhere npx runs directly and arguments are passed verbatim.
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val cmd = if (isWindows) mutableListOf("cmd", "/c") else mutableListOf()

        // Pinned to the spec version ktoon targets, so golden regeneration is deterministic.
        cmd.addAll(listOf("npx", "@toon-format/cli@4.1.1", json.name, "-o", toon.name))

        if (ktoon.configuration.delimiter != KtoonConfiguration.Delimiter.COMMA) {
            val delimiter = ktoon.configuration.delimiter.char.toString()
            cmd.add("--delimiter")
            cmd.add(if (isWindows) "\"$delimiter\"" else delimiter)
        }
        if (ktoon.configuration.indentSize != 2) {
            cmd.add("--indent")
            cmd.add(ktoon.configuration.indentSize.toString())
        }

        val process =
            ProcessBuilder().command(cmd).directory(toon.parent.toFile()).inheritIO().start()
        // Generous timeout: a cold npx run downloads the CLI package before converting.
        if (!process.waitFor(1, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            error("Golden regeneration timed out. Command: ${cmd.joinToString(" ")}")
        }
        if (process.exitValue() != 0 || !toon.isReadable()) {
            error(
                "Golden regeneration failed (exit ${process.exitValue()}, requires npm). " +
                    "Command: ${cmd.joinToString(" ")}"
            )
        }
        // The v4 CLI appends a trailing newline; goldens are stored without one.
        toon.writeText(toon.readText().trimEnd('\n', '\r'))
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
