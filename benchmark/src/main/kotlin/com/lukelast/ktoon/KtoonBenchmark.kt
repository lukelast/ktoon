package com.lukelast.ktoon

import br.com.vexpera.ktoon.Toon
import dev.toonformat.jtoon.JToon
import kotlinx.benchmark.*
import kotlinx.serialization.Serializable
import org.instancio.Instancio
import org.instancio.settings.Keys

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
                .withSetting(Keys.COLLECTION_MIN_SIZE, 200)
                .withSetting(Keys.COLLECTION_MAX_SIZE, 200)
                .withSetting(Keys.STRING_MIN_LENGTH, 25)
                .withSetting(Keys.STRING_MAX_LENGTH, 50)
                .withSetting(Keys.LONG_MIN, 100)
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
    fun benchmarkToonKotlin(): String{
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
