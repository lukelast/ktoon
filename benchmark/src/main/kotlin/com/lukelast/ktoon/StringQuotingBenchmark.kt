package com.lukelast.ktoon

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

/** Length of the "long_string" input case. */
private const val LONG_STRING_LENGTH = 1000

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class StringQuotingBenchmark {
    @Param(
        "simple",
        "numeric_like_123",
        "numeric_like_05",
        "special_chars",
        "long_string",
        "key_valid",
        "key_invalid",
    )
    var inputType: String = ""
    private var inputString: String = ""
    private var isKey: Boolean = false

    @Setup
    fun setup() {
        inputString =
            when (inputType) {
                "simple" -> "simpleString"
                "numeric_like_123" -> "12345"
                "numeric_like_05" -> "05"
                "special_chars" -> "string:with[special]chars"
                "long_string" -> "a".repeat(LONG_STRING_LENGTH)
                "key_valid" -> "valid.key_name"
                "key_invalid" -> "invalid key name"
                else -> "default"
            }

        isKey = inputType.startsWith("key")
    }

    @Benchmark
    fun needsQuoting(): Boolean {
        return if (isKey) {
            BenchmarkAccess.needsQuotingForKey(inputString)
        } else {
            BenchmarkAccess.needsQuotingForValue(inputString)
        }
    }
}
