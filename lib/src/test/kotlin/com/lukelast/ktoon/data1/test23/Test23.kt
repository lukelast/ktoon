package com.lukelast.ktoon.data1.test23

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Disabled

/**
 * Test23: Very large numbers and Int64 limits (§2)
 * Tests canonical decimal form for edge-case integers and longs
 * Expected: No exponent notation, all numbers in decimal form
 */
@Disabled("TOON CLI doesn't support this yet")
class Test23 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class LargeNumbersData(
    val maxLong: Long,
    val minLong: Long,
    val maxInt: Int,
    val minInt: Int,
    val veryLargeDouble: Double,
    val largeNegativeDouble: Double,
    val almostMaxLong: Long,
    val nearZero: Long
)

val data = LargeNumbersData(
    maxLong = Long.MAX_VALUE,              // 9223372036854775807
    minLong = Long.MIN_VALUE,              // -9223372036854775808
    maxInt = Int.MAX_VALUE,                // 2147483647
    minInt = Int.MIN_VALUE,                // -2147483648
    veryLargeDouble = 1.7976931348623157e308,  // Near Double.MAX_VALUE
    largeNegativeDouble = -1.7976931348623157e308,
    almostMaxLong = Long.MAX_VALUE - 1,    // 9223372036854775806
    nearZero = 0L
)
