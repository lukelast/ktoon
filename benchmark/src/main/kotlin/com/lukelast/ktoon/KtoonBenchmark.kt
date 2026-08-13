package com.lukelast.ktoon

import br.com.vexpera.ktoon.Toon
import dev.toonformat.jtoon.JToon
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import kotlinx.serialization.Serializable
import org.instancio.Instancio
import org.instancio.settings.Keys

// Shape of the generated benchmark payload: fixed so runs are comparable.
private const val COLLECTION_SIZE = 200
private const val MIN_STRING_LENGTH = 25
private const val MAX_STRING_LENGTH = 50
private const val MIN_LONG_VALUE = 100L

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = BenchmarkTimeUnit.SECONDS)
open class KtoonBenchmark {
    private lateinit var data: BenchmarkData
    private val ktoon = Ktoon.Default

    @Setup
    fun setup() {
        data =
            Instancio.of(BenchmarkData::class.java)
                .withSeed(0)
                .withSetting(Keys.COLLECTION_MIN_SIZE, COLLECTION_SIZE)
                .withSetting(Keys.COLLECTION_MAX_SIZE, COLLECTION_SIZE)
                .withSetting(Keys.STRING_MIN_LENGTH, MIN_STRING_LENGTH)
                .withSetting(Keys.STRING_MAX_LENGTH, MAX_STRING_LENGTH)
                .withSetting(Keys.LONG_MIN, MIN_LONG_VALUE)
                .withSetting(Keys.LONG_MAX, Int.MAX_VALUE.toLong())
                .create()
    }

    @Benchmark
    @Warmup(iterations = 4, time = 2, timeUnit = BenchmarkTimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 6, timeUnit = BenchmarkTimeUnit.SECONDS)
    //    @org.openjdk.jmh.annotations.Fork(value = 1, jvmArgsAppend = [JFR_ARGS])
    fun benchmarkKtoon(): String {
        return ktoon.encodeToString(data)
    }

    @Benchmark
    fun benchmarkJtoon(): String {
        return JToon.encode(data)
    }

    @Benchmark
    fun benchmarkToonKotlin(): String {
        return Toon.encode(data)
    }
}

@Serializable
data class BenchmarkData(
    val name: String,
    val id: Long,
    val items: List<String>,
    val nested: NestedData,
    val moreItems: List<Long>,
    val rows: List<Row>,
)

@Serializable data class Row(val id: Long, val active: Boolean, val name: String, val value: String)

@Serializable data class NestedData(val description: String, val active: Boolean, val score: Double)

private const val JFR_ARGS =
    "-XX:StartFlightRecording=filename=../benchmark.jfr,settings=profile,dumponexit=true,jdk.ExecutionSample#period=2ms"
