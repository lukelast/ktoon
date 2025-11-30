package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * JSON parser for deserializing fixture inputs.
 */
private val fixtureInputJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
}

/**
 * Helper function to run a fixture test with a typed data class.
 *
 * @param fixtureName Fixture file name without path or extension (e.g., "primitives")
 * @param testName Name of the test case in the fixture
 * @param deserializer Deserializer for the input type
 * @param serializer Serializer for encoding with Ktoon
 */
fun <T> runFixtureTest(
    fixtureName: String,
    testName: String,
    deserializer: DeserializationStrategy<T>,
    serializer: SerializationStrategy<T>
) {
    // Construct full fixture path
    val fixturePath = "fixtures/encode/$fixtureName.json"

    // Load fixture and find test case
    val fixture = loadFixture(fixturePath)
    val testCase = fixture.tests.find { it.name == testName }
        ?: error("Test case '$testName' not found in $fixturePath")

    // Deserialize input from JsonElement to typed data class
    val input = fixtureInputJson.decodeFromJsonElement(deserializer, testCase.input)

    // Create Ktoon with test options
    val config = testCase.options.toToonConfiguration()
    val ktoon = Ktoon(configuration = config)

    // Encode with Ktoon
    val encoded = ktoon.encodeToString(serializer, input)

    // Compare with expected
    val expected = testCase.expected.asString()
    assertEquals(
        expected,
        encoded,
        buildString {
            append("Test '$testName' failed")
            testCase.note?.let { append("\nNote: $it") }
            testCase.specSection?.let { append("\nSpec: §$it") }
        }
    )
}

/**
 * Inline version with reified types for convenience.
 */
inline fun <reified T> runFixtureTest(
    fixture: String,
    testName: String
) {
    runFixtureTest(
        fixture,
        testName,
        serializer<T>(),
        serializer<T>()
    )
}


fun JsonElement.asString(): String {
    return (this as? JsonPrimitive)?.content
        ?: throw IllegalArgumentException("Expected string JsonElement, got $this")
}