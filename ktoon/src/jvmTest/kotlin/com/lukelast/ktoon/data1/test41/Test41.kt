package com.lukelast.ktoon.data1.test41

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/**
 * Test41: Canonical-range number formatting (§2)
 *
 * Every number here sits inside the canonical decimal range (n = 0, or 1e-6 ≤ |n| < 1e21), so the
 * encoder MUST emit plain decimal with no exponent notation:
 * - 1e6 → 1000000 and 1e-6 → 0.000001 (no exponent notation inside the canonical range)
 * - 1e20 → 100000000000000000000 (still below the 1e21 cutoff)
 * - Zero fractional part collapses to integer form (100.0 → 100)
 * - Full double precision is preserved (0.3333333333333333, 0.30000000000000004)
 * - Int and in-2^53 Long values pass through unchanged
 *
 * The canonical form is position-independent, so the same values are repeated as object field
 * values, inline array elements, and tabular cells.
 *
 * Negative zero is deliberately excluded here; it is covered by test25.
 */
class Test41 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class CanonicalRow(
    val label: String,
    val value: Double,
    val count: Int,
    val ratio: Double,
)

@Serializable
data class CanonicalNumbersData(
    val million: Double,
    val millionth: Double,
    val mixedDecimal: Double,
    val negativePi: Double,
    val half: Double,
    val oneAndHalf: Double,
    val oneThird: Double,
    val floatArtifact: Double,
    val hundred: Double,
    val hugeCanonical: Double,
    val answer: Int,
    val negativeInt: Int,
    val maxInt: Int,
    val maxSafeLong: Long,
    val inlineDoubles: List<Double>,
    val inlineLongs: List<Long>,
    val rows: List<CanonicalRow>,
)

val data =
    CanonicalNumbersData(
        million = 1.0e6, // JSON 1000000.0 → 1000000
        millionth = 0.000001, // JSON 1.0E-6 → 0.000001 (lower canonical bound)
        mixedDecimal = 123.456, // → 123.456
        negativePi = -3.14, // → -3.14
        half = 0.5, // → 0.5 (leading zero kept)
        oneAndHalf = 1.5, // → 1.5
        oneThird = 0.3333333333333333, // → 0.3333333333333333 (full precision)
        floatArtifact = 0.30000000000000004, // → 0.30000000000000004 (0.1 + 0.2 artifact)
        hundred = 100.0, // JSON 100.0 → 100
        hugeCanonical = 1.0e20, // JSON 1.0E20 → 100000000000000000000
        answer = 42, // → 42
        negativeInt = -7, // → -7
        maxInt = Int.MAX_VALUE, // → 2147483647
        maxSafeLong = 9007199254740991L, // → 9007199254740991 (2^53 - 1)

        // Inline array position: same canonical forms, comma separated.
        // → 1000000,0.000001,123.456,-3.14,0.5,1.5,100,100000000000000000000
        inlineDoubles = listOf(1.0e6, 0.000001, 123.456, -3.14, 0.5, 1.5, 100.0, 1.0e20),
        // → 42,-7,2147483647,9007199254740991
        inlineLongs = listOf(42L, -7L, 2147483647L, 9007199254740991L),

        // Tabular cell position: canonical form is identical inside rows.
        rows =
            listOf(
                // → alpha,1000000,42,0.3333333333333333
                CanonicalRow(
                    label = "alpha",
                    value = 1.0e6,
                    count = 42,
                    ratio = 0.3333333333333333,
                ),
                // → beta,0.000001,-7,0.30000000000000004
                CanonicalRow(
                    label = "beta",
                    value = 0.000001,
                    count = -7,
                    ratio = 0.30000000000000004,
                ),
                // → gamma,100,2147483647,100000000000000000000
                CanonicalRow(
                    label = "gamma",
                    value = 100.0,
                    count = Int.MAX_VALUE,
                    ratio = 1.0e20,
                ),
            ),
    )
