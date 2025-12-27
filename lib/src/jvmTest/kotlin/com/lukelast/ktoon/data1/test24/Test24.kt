package com.lukelast.ktoon.data1.test24

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable
import kotlin.test.Ignore

/**
 * Test24: Very small floats and precision edge cases (§2)
 * Tests canonical decimal form for small and subnormal values
 * Expected: Trailing zeros stripped (1.5000 → 1.5), canonical form maintained
 */
@Ignore("TOON CLI doesn't support this yet")
class Test24 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class SmallFloatsData(
    val tinyFloat: Double,
    val subnormal: Double,
    val almostZero: Double,
    val precisionTest: Double,
    val microValue: Double,
    val nanoValue: Double,
    val standardSmall: Double,
    val repeatingDecimal: Double
)

val data = SmallFloatsData(
    tinyFloat = 0.000001,                    // 1e-6
    subnormal = Double.MIN_VALUE,            // 4.9e-324 (smallest positive subnormal)
    almostZero = 1e-308,                     // Near smallest normal
    precisionTest = 0.3333333333333333,      // Repeating decimal
    microValue = 0.000000001,                // 1e-9
    nanoValue = 0.000000000001,              // 1e-12
    standardSmall = 0.0001,                  // 1e-4
    repeatingDecimal = 1.0 / 3.0             // 0.333...
)
