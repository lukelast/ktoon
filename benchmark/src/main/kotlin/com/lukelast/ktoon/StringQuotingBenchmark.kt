package com.lukelast.ktoon

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
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
                "long_string" -> "a".repeat(1000)
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
