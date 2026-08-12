package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonException
import com.lukelast.ktoon.decoding.ToonLexer
import com.lukelast.ktoon.decoding.ToonReader
import com.lukelast.ktoon.decoding.ToonValue
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

/**
 * Runs every case in every fixture file, so fixture coverage never depends on a hand-written test
 * existing for each case.
 *
 * Encode cases go through [Ktoon.encodeJsonToToon]. Decode cases go through the internal
 * lexer/reader (there is no public generic TOON→JSON API yet) and compare against the expected JSON
 * using the spec's JSON-model equality: ordered object keys, mathematical number equality.
 */
class FixtureSweepTest {

    @TestFactory
    fun `encode fixtures`(): List<DynamicTest> =
        loadEncodeFixtures().flatMap { (file, fixture) ->
            fixture.tests.map { case ->
                DynamicTest.dynamicTest("$file :: ${case.name}") {
                    val ktoon = Ktoon(configuration = case.options.toToonConfiguration())
                    if (case.shouldError) {
                        assertThrows<KtoonException> { ktoon.encodeJsonToToon(case.input) }
                    } else {
                        val actual = ktoon.encodeJsonToToon(case.input)
                        assertEquals(case.expected.asString(), actual, failureContext(case))
                    }
                }
            }
        }

    @TestFactory
    fun `decode fixtures`(): List<DynamicTest> =
        loadDecodeFixtures().flatMap { (file, fixture) ->
            fixture.tests.map { case ->
                DynamicTest.dynamicTest("$file :: ${case.name}") {
                    val config = case.options.toToonConfiguration()
                    if (case.shouldError) {
                        assertThrows<KtoonException> { decodeToJson(case.input.asString(), config) }
                    } else {
                        val actual = decodeToJson(case.input.asString(), config)
                        assertEquals(
                            canonical(case.expected),
                            canonical(actual),
                            failureContext(case),
                        )
                    }
                }
            }
        }

    private fun failureContext(case: FixtureTestCase): String = buildString {
        append("Fixture case '${case.name}'")
        case.specSection?.let { append("\nSpec: §$it") }
        case.note?.let { append("\nNote: $it") }
    }

    private fun decodeToJson(
        toon: String,
        config: KtoonConfiguration,
    ): JsonElement {
        val root =
            try {
                ToonReader(ToonLexer(toon, config).tokenize(), config).readRoot()
            } catch (e: KtoonException) {
                throw e
            } catch (e: Exception) {
                fail("Parser crashed with ${e::class.simpleName}: ${e.message}", e)
            }
        return root.toJsonElement()
    }

    private fun ToonValue.toJsonElement(): JsonElement =
        when (this) {
            is ToonValue.Null -> JsonNull
            is ToonValue.Boolean -> JsonPrimitive(value)
            is ToonValue.Number -> JsonPrimitive(value)
            is ToonValue.String -> JsonPrimitive(value)
            is ToonValue.Object -> JsonObject(properties.mapValues { (_, v) -> v.toJsonElement() })
            is ToonValue.Array -> JsonArray(elements.map { it.toJsonElement() })
        }

    /**
     * Canonical text form implementing the spec's JSON-model equality (§2): object key order is
     * significant, numbers compare by mathematical value (300.0 == 300), strings by exact content.
     */
    private fun canonical(element: JsonElement): String = buildString { canonicalize(element) }

    private fun StringBuilder.canonicalize(element: JsonElement) {
        when (element) {
            is JsonNull -> append("null")
            is JsonPrimitive ->
                if (element.isString) {
                    append(JsonPrimitive(element.content).toString())
                } else {
                    val content = element.content
                    if (content == "true" || content == "false") {
                        append(content)
                    } else {
                        append(BigDecimal(content).stripTrailingZeros().toPlainString())
                    }
                }
            is JsonObject -> {
                append('{')
                element.entries.forEachIndexed { i, (k, v) ->
                    if (i > 0) append(',')
                    append(JsonPrimitive(k).toString())
                    append(':')
                    canonicalize(v)
                }
                append('}')
            }
            is JsonArray -> {
                append('[')
                element.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    canonicalize(v)
                }
                append(']')
            }
        }
    }
}
