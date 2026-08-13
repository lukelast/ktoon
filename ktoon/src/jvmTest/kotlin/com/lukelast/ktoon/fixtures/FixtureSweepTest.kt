package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import com.lukelast.ktoon.KtoonException
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
 * Encode cases go through [Ktoon.encodeJsonToToon] and decode cases through
 * [Ktoon.decodeToonToJson], comparing against the expected JSON using the spec's JSON-model
 * equality: ordered object keys, mathematical number equality.
 */
class FixtureSweepTest {

    @TestFactory
    fun `encode fixtures`(): List<DynamicTest> =
        loadEncodeFixtures().flatMap { (file, fixture) ->
            fixture.tests.map { case ->
                DynamicTest.dynamicTest("$file :: ${case.name}") {
                    val ktoon = Ktoon(configuration = case.options.toToonConfiguration())
                    if (case.shouldError) {
                        assertToonError { ktoon.encodeJsonToToon(case.input) }
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
                    val ktoon = Ktoon(configuration = case.options.toToonConfiguration())
                    if (case.shouldError) {
                        assertToonError { ktoon.decodeToonToJson(case.input.asString()) }
                    } else {
                        val actual = ktoon.decodeToonToJson(case.input.asString())
                        assertEquals(
                            canonical(case.expected),
                            canonical(actual),
                            failureContext(case),
                        )
                    }
                }
            }
        }

    /**
     * Asserts the block fails with a genuine TOON error. The public API wraps unexpected crashes in
     * a KtoonException with the original exception as cause, so a non-Ktoon cause means the
     * implementation crashed rather than rejected the input.
     */
    private fun assertToonError(block: () -> Unit) {
        val error = assertThrows<KtoonException>(block)
        val cause = error.cause
        if (cause != null && cause !is KtoonException) {
            fail("Implementation crashed with ${cause::class.simpleName}: ${cause.message}", error)
        }
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
