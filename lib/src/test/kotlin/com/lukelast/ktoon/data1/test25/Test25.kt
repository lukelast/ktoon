package com.lukelast.ktoon.data1.test25

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test25: Negative zero and special number formatting (§2)
 * Tests:
 * - -0 normalizes to 0
 * - Fractional zero stripped (4.0 → 4)
 * - Trailing zeros removed (1.5000 → 1.5)
 * - No leading-zero violations (0.5 not .5)
 */
class Test25 : Runner() {
    override fun run() = doTest(data, testDecode = false)
}

@Serializable
data class SpecialNumbersData(
    val negativeZero: Double,
    val positiveZero: Double,
    val integerFromFloat: Double,
    val trailingZeros: Double,
    val noLeadingZeros: Double,
    val negativeNoLeadingZeros: Double,
    val exactInteger: Double,
    val halfValue: Double,
    val quarterValue: Double,
    val zeroInt: Int,
    val zeroLong: Long
)

val data = SpecialNumbersData(
    negativeZero = -0.0,                     // Should encode as 0
    positiveZero = 0.0,                      // Should encode as 0
    integerFromFloat = 4.0,                  // Should encode as 4
    trailingZeros = 1.5000,                  // Should encode as 1.5
    noLeadingZeros = 0.5,                    // Should encode as 0.5 (not .5)
    negativeNoLeadingZeros = -0.5,           // Should encode as -0.5 (not -.5)
    exactInteger = 100.0,                    // Should encode as 100
    halfValue = 0.5,                         // Already canonical
    quarterValue = 0.25,                     // Already canonical
    zeroInt = 0,                             // Integer zero
    zeroLong = 0L                            // Long zero
)
