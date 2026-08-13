package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonException
import com.lukelast.ktoon.data1.jsonPretty
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

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
    val testCase = findTestCase(FixtureCategory.ENCODE, fixtureName, testName)

    // Deserialize input from JsonElement to typed data class
    val input = fixtureJson.decodeFromJsonElement(deserializer, testCase.input)
    val ktoon = Ktoon(configuration = testCase.options.toToonConfiguration())

    if (testCase.shouldError) {
        // Test expects an error to be thrown
        assertFailsWith<KtoonException> { ktoon.encodeToString(serializer, input) }
    } else {
        assertEquals(
            testCase.expected.asString(),
            ktoon.encodeToString(serializer, input),
            failureContext(testCase),
        )
    }
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
    val testCase = findTestCase(FixtureCategory.DECODE, fixtureName, testName)
    val ktoon = Ktoon(configuration = testCase.options.toToonConfiguration())
    val toonInput = testCase.input.asString()

    if (testCase.shouldError) {
        // Test expects an error to be thrown
        assertFailsWith<KtoonException> { ktoon.decodeFromString(deserializer, toonInput) }
    } else {
        // Decode TOON to typed value
        val actualObject = ktoon.decodeFromString(deserializer, toonInput)
        val expectedObject = fixtureJson.decodeFromJsonElement(deserializer, testCase.expected)

        val message = failureContext(testCase)
        // Compare JSON renderings first for a readable diff, then the objects themselves so a
        // difference that doesn't survive JSON serialization still fails.
        assertEquals(
            jsonPretty.encodeToString(serializer, expectedObject),
            jsonPretty.encodeToString(serializer, actualObject),
            message,
        )
        assertEquals(expectedObject, actualObject, message)
    }
}

inline fun <reified T> runFixtureDecodeTest(
    fixture: String,
    testName: String = currentFixtureTestName(),
) {
    runFixtureDecodeTest(fixture, testName, serializer<T>(), serializer<T>())
}

private fun findTestCase(
    category: FixtureCategory,
    fixtureName: String,
    testName: String,
): FixtureTestCase {
    val directory =
        when (category) {
            FixtureCategory.ENCODE -> "encode"
            FixtureCategory.DECODE -> "decode"
        }
    val fixturePath = "fixtures/$directory/$fixtureName.json"
    return loadFixture(fixturePath).tests.find { it.name == testName }
        ?: error("Test case '$testName' not found in $fixturePath")
}

/** Assertion message carrying the fixture case's name, spec reference, and note. */
fun failureContext(case: FixtureTestCase): String = buildString {
    append("Fixture case '${case.name}' failed")
    case.specSection?.let { append("\nSpec: §$it") }
    case.note?.let { append("\nNote: $it") }
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

fun JsonElement.asString(): String {
    return (this as? JsonPrimitive)?.content
        ?: throw IllegalArgumentException("Expected string JsonElement, got $this")
}
