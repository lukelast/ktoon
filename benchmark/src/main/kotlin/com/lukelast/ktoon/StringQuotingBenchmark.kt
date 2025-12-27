package com.lukelast.ktoon

import com.lukelast.ktoon.encoding.StringQuoting
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

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
    private var context: StringQuoting.QuotingContext = StringQuoting.QuotingContext.OBJECT_VALUE

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

        context =
            if (inputType.startsWith("key")) {
                StringQuoting.QuotingContext.OBJECT_KEY
            } else {
                StringQuoting.QuotingContext.OBJECT_VALUE
            }
    }

    @Benchmark
    fun needsQuoting(): Boolean {
        return StringQuoting.needsQuoting(inputString, context)
    }
}
