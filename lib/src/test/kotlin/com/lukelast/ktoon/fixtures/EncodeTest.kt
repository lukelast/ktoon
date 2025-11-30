package com.lukelast.ktoon.fixtures

import com.lukelast.ktoon.Ktoon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class EncodeTest {

    @Test
    @Disabled
    fun testEncode() {
        val fixtures = loadEncodeFixtures()
        for ((filename, fixture) in fixtures) {
            for (testCase in fixture.tests) {
                val input = testCase.input
                val expected = testCase.expected.asString()
                val options = testCase.options
                val ktoonConfig = testCase.options.toToonConfiguration()
                val ktoon = Ktoon(configuration = ktoonConfig)

                // TODO: Convert input JsonElement to appropriate Kotlin type before encoding
                val data = null
                // TODO encode data class to TOON string
                val encodedToon = ""
                assertEquals(expected, encodedToon)
            }
        }
    }
}
