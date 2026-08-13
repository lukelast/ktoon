package com.lukelast.ktoon.fixtures

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

/** Strict JSON for fixture files and their typed inputs, so schema mismatches fail loudly. */
val fixtureJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
}

private val fixtureCache = ConcurrentHashMap<String, ToonFixture>()

/** Loads and parses a fixture file, cached because many tests share the same file. */
fun loadFixture(resourcePath: String): ToonFixture =
    fixtureCache.getOrPut(resourcePath) {
        fixtureJson.decodeFromString<ToonFixture>(loadResourceFile(resourcePath))
    }

fun loadResourceFile(resourcePath: String): String {
    val url =
        ToonFixture::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
    return url.readText()
}

fun loadAllFixtures(directoryPath: String): Map<String, ToonFixture> {
    val resourceUrl =
        ToonFixture::class.java.classLoader.getResource(directoryPath)
            ?: throw IllegalArgumentException("Directory not found: $directoryPath")

    val directory = File(resourceUrl.toURI())
    require(directory.isDirectory) { "$directoryPath is not a directory" }

    return directory
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .associate { file -> file.name to loadFixture("$directoryPath/${file.name}") }
}

fun loadEncodeFixtures(): Map<String, ToonFixture> {
    return loadAllFixtures("fixtures/encode")
}

fun loadDecodeFixtures(): Map<String, ToonFixture> {
    return loadAllFixtures("fixtures/decode")
}
