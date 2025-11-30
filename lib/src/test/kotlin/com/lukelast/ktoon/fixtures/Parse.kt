package com.lukelast.ktoon.fixtures

import kotlinx.serialization.json.Json
import java.io.File

private val fixtureJson = Json {
    ignoreUnknownKeys = false // Strict: catch schema mismatches
    isLenient = true // Allow some parsing flexibility
    prettyPrint = false
}

fun loadFixture(resourcePath: String): ToonFixture {
    val content = loadResourceFile(resourcePath)
    return fixtureJson.decodeFromString<ToonFixture>(content)
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
        ?.associate { file -> file.name to loadFixture("$directoryPath/${file.name}") }
        ?: emptyMap()
}

fun loadEncodeFixtures(): Map<String, ToonFixture> {
    return loadAllFixtures("fixtures/encode")
}

fun loadDecodeFixtures(): Map<String, ToonFixture> {
    return loadAllFixtures("fixtures/decode")
}
