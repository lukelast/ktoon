package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonException
import com.lukelast.ktoon.data1.jsonPretty
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

/**
 * Helper function to run a fixture test with a typed data class.
 *
 * @param fixtureName Fixture file name without path or extension (e.g., "primitives")
 * @param testName Name of the test case in the fixture
 * @param deserializer Deserializer for the input type
 * @param serializer Serializer for encoding with Ktoon
 */
fun <T> runFixtureEncodeTest(
    fixtureName: String,
    testName: String,
    deserializer: DeserializationStrategy<T>,
    serializer: SerializationStrategy<T>,
) {
    // Construct full fixture path
    val fixturePath = "fixtures/encode/$fixtureName.json"

    // Load fixture and find test case
    val fixture = loadFixture(fixturePath)
    val testCase =
        fixture.tests.find { it.name == testName }
            ?: error("Test case '$testName' not found in $fixturePath")

    // Deserialize input from JsonElement to typed data class
    val input = fixtureInputJson.decodeFromJsonElement(deserializer, testCase.input)

    // Create Ktoon with test options
    val config = testCase.options.toToonConfiguration()
    val ktoon = Ktoon(configuration = config)

    // Encode with Ktoon
    val ktoonObjectToToon = ktoon.encodeToString(serializer, input)

    // Compare with expected
    val expectedToon = testCase.expected.asString()
    assertEquals(
        expectedToon,
        ktoonObjectToToon,
        buildString {
            append("Test '$testName' failed")
            testCase.note?.let { append("\nNote: $it") }
            testCase.specSection?.let { append("\nSpec: §$it") }
        },
    )

    val ktoonJsonToToon = ktoon.encodeJsonToToon(testCase.input)
    assertEquals(
        expectedToon,
        ktoonJsonToToon,
        buildString {
            append("Test '$testName' failed")
            append("\nktoon.encodeJsonToToon()")
            testCase.note?.let { append("\nNote: $it") }
            testCase.specSection?.let { append("\nSpec: §$it") }
        },
    )
}

inline fun <reified T> runFixtureEncodeTest(
    fixture: String,
    testName: String = currentFixtureTestName(),
) {
    runFixtureEncodeTest(fixture, testName, serializer<T>(), serializer<T>())
}

/**
 * Helper function to run a decode fixture test with a typed data class.
 *
 * @param fixtureName Fixture file name without path or extension (e.g., "primitives")
 * @param testName Name of the test case in the fixture
 * @param deserializer Deserializer for decoding from Ktoon
 * @param serializer Serializer for encoding to JSON
 */
fun <T> runFixtureDecodeTest(
    fixtureName: String,
    testName: String,
    deserializer: DeserializationStrategy<T>,
    serializer: SerializationStrategy<T>,
) {
    // Construct full fixture path
    val fixturePath = "fixtures/decode/$fixtureName.json"

    // Load fixture and find test case
    val fixture = loadFixture(fixturePath)
    val testCase =
        fixture.tests.find { it.name == testName }
            ?: error("Test case '$testName' not found in $fixturePath")

    // Create Ktoon with test options
    val config = testCase.options.toToonConfiguration()
    val ktoon = Ktoon(configuration = config)

    // Get TOON input string
    val toonInput = testCase.input.asString()

    if (testCase.shouldError) {
        // Test expects an error to be thrown
        assertThrows<KtoonException> { ktoon.decodeFromString(deserializer, toonInput) }
    } else {
        // Decode TOON to typed value
        val actualObject = ktoon.decodeFromString(deserializer, toonInput)
        val actualJsonText = jsonPretty.encodeToString(serializer, actualObject)

        val expectedObject = fixtureInputJson.decodeFromJsonElement(deserializer, testCase.expected)
        val expectedJsonText = jsonPretty.encodeToString(serializer, expectedObject)

        // If objects aren't equal then compare the json strings to get a nice diff.
        if (expectedObject != actualObject) {
            assertEquals(
                expectedJsonText,
                actualJsonText,
                buildString {
                    append("Test '$testName' failed")
                    testCase.note?.let { append("\nNote: $it") }
                    testCase.specSection?.let { append("\nSpec: §$it") }
                },
            )
        }
    }
}

inline fun <reified T> runFixtureDecodeTest(
    fixture: String,
    testName: String = currentFixtureTestName(),
) {
    runFixtureDecodeTest(fixture, testName, serializer<T>(), serializer<T>())
}

fun currentFixtureTestName(): String {
    val encodePackage = "com.lukelast.ktoon.fixtures.encode"
    val decodePackage = "com.lukelast.ktoon.fixtures.decode"
    return Thread.currentThread()
        .stackTrace
        .firstOrNull {
            it.className.startsWith(encodePackage) || it.className.startsWith(decodePackage)
        }
        ?.methodName
        ?: error(
            "Unable to determine fixture test name from stack trace; " +
                "ensure calls originate from $encodePackage or $decodePackage"
        )
}

/** JSON parser for deserializing fixture inputs. */
private val fixtureInputJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
}

fun JsonElement.asString(): String {
    return (this as? JsonPrimitive)?.content
        ?: throw IllegalArgumentException("Expected string JsonElement, got $this")
}
