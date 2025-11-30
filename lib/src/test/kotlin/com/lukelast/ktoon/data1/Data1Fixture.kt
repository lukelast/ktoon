package com.lukelast.ktoon.data1

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.ToonConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

inline fun <reified T> doTest(
    test: Any,
    data: T,
    ktoonConfig: ToonConfiguration = ToonConfiguration.Default,
) {
    val ktoon = Ktoon(configuration = ktoonConfig)
    val rawJson = readFile(test.javaClass, "data.json")
    val rawToon = readFile(test.javaClass, "data.toon")

    val dataToJson = json.encodeToString(data)
    assertEquals(rawJson, dataToJson, "JSON serialization did not match expected output")

    val dataToToon = ktoon.encodeToString(data)
    assertEquals(rawToon, dataToToon)

    val rawJsonToData: T = json.decodeFromString(rawJson)
    assertEquals(data, rawJsonToData)

    //    val toonDecoded = Ktoon().decodeFromString<T>(toonData)
    //    assertEquals(data, toonDecoded)
}

fun readFile(clazz: Class<*>, fileName: String): String {
    val basePath = Paths.get("src", "test", "kotlin")
    val packagePath = Paths.get(clazz.`package`.name.replace('.', '/'))
    val fullPath: Path = basePath.resolve(packagePath).resolve(fileName)
    return fullPath.toFile().readText()
}
