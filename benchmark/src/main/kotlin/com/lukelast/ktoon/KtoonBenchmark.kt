@file:Suppress("MagicNumber") // Fixed fixtures: the literal values are the data.

package com.lukelast.ktoon

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** Rows per table, enough that per-row work dominates the fixed cost of a call. */
private const val TABLE_ROWS = 200

/** Elements per inline primitive array. */
private const val SERIES_POINTS = 500

/** Levels in the deep-nesting payload, well under the default 128-container encoder limit. */
private const val NESTING_DEPTH = 50

/**
 * Encoder benchmarks for ktoon alone, one per path that costs real time, for performance work on
 * the library itself. Cross-library timings belong in [ComparisonBenchmark] instead.
 *
 * Every dataset is fixed and hard-coded, so a run measures an encoder change rather than a change
 * in the data. Each benchmark is a separate method rather than one method over a `@Param`, so its
 * call site stays monomorphic and profiles for one path in isolation.
 *
 * Serializers are resolved once up front: what varies between runs should be encoding, not the
 * serializer lookup that precedes it.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = BenchmarkTimeUnit.SECONDS)
open class KtoonBenchmark {
    private val ktoon = Ktoon.Default

    private val logRows = cycle(LOG_ROW_SEED, TABLE_ROWS)
    private val logRowSerializer = ListSerializer(LogRow.serializer())

    private val readings = cycle(READING_SEED, TABLE_ROWS)
    private val readingSerializer = ListSerializer(Reading.serializer())

    private val orders = cycle(ORDER_SEED, TABLE_ROWS)
    private val orderSerializer = ListSerializer(Order.serializer())

    private val notes = cycle(NOTE_SEED, TABLE_ROWS)
    private val noteSerializer = ListSerializer(Note.serializer())

    private val samples = cycle(SAMPLE_SEED, TABLE_ROWS)
    private val sampleSerializer = ListSerializer(Sample.serializer())

    private val series =
        Series(
            name = "cpu utilization",
            ticks = cycle(TICK_SEED, SERIES_POINTS),
            values = cycle(VALUE_SEED, SERIES_POINTS),
            labels = cycle(LABEL_SEED, SERIES_POINTS),
        )

    private val nodeChain = buildNodeChain(NESTING_DEPTH)

    /**
     * §9.3 tabular: a uniform array of flat objects, the shape TOON exists to compress. Covers
     * tabular detection over every row and column, then the header and row writes. Values are
     * chosen to need no quoting, so this measures the tabular machinery rather than escaping.
     */
    @Benchmark fun encodeTabular(): String = ktoon.encodeToString(logRowSerializer, logRows)

    /**
     * §9.3 with a nested-uniform column, which recurses through field-tree detection, emits a
     * `{a,b{c,d}}` grouped header, and walks the group again for every row's cells.
     */
    @Benchmark fun encodeTabularNested(): String = ktoon.encodeToString(readingSerializer, readings)

    /**
     * Tabular detection that fails and falls back to list form (§9.2): each row holds an array
     * field, so no field tree exists. The detection cost is paid in full and thrown away before
     * every row is written the long way — the case where wasted work is the whole cost.
     */
    @Benchmark fun encodeListFallback(): String = ktoon.encodeToString(orderSerializer, orders)

    /** §9.5 keyed tabular: an object whose fields are themselves uniform objects. */
    @Benchmark
    fun encodeKeyedTabular(): String = ktoon.encodeToString(ServiceMetrics.serializer(), SERVICES)

    /**
     * A wide object of primitives, which takes the streaming `ToonObjectEncoder` path and captures
     * nothing — the one encoder path that never builds an intermediate element tree.
     */
    @Benchmark
    fun encodeWideObject(): String = ktoon.encodeToString(ServerConfig.serializer(), SERVER_CONFIG)

    /**
     * Deeply nested objects: encoder recursion, the nesting-depth check, and growing indentation.
     */
    @Benchmark fun encodeDeepNesting(): String = ktoon.encodeToString(Node.serializer(), nodeChain)

    /**
     * Strings that all require quoting: quotes, backslashes, control characters, delimiters,
     * numeric-like text and reserved words. Forces the escaping builder in `StringQuoting.quote`
     * for every value, where [encodeTabular] never enters it.
     */
    @Benchmark fun encodeQuotedStrings(): String = ktoon.encodeToString(noteSerializer, notes)

    /**
     * Number-heavy rows covering each `NumberNormalizer` branch: integral doubles, plain fractions,
     * and magnitudes whose `toString` is scientific notation and must be expanded to plain decimal.
     */
    @Benchmark fun encodeNumbers(): String = ktoon.encodeToString(sampleSerializer, samples)

    /** Long inline primitive arrays, which skip form detection and stream straight out. */
    @Benchmark fun encodeInlineArrays(): String = ktoon.encodeToString(Series.serializer(), series)
}

/** Repeats [seed] in order up to [size] elements, so every dataset stays fixed and reproducible. */
private fun <T> cycle(seed: List<T>, size: Int): List<T> = List(size) { seed[it % seed.size] }

private fun buildNodeChain(depth: Int): Node {
    var node = Node(NODE_NAMES[0], depth, null)
    for (level in depth - 1 downTo 1) {
        node = Node(NODE_NAMES[level % NODE_NAMES.size], level, node)
    }
    return node
}

// ----- tabular -----

@Serializable
data class LogRow(
    val id: Long,
    val level: String,
    val service: String,
    val message: String,
    val durationMs: Int,
    val ok: Boolean,
)

private val LOG_ROW_SEED =
    listOf(
        LogRow(90001, "INFO", "auth", "session established", 12, true),
        LogRow(90002, "WARN", "billing", "retrying charge attempt", 148, true),
        LogRow(90003, "ERROR", "search", "index shard unavailable", 2044, false),
        LogRow(90004, "INFO", "cart", "item added to cart", 7, true),
        LogRow(90005, "DEBUG", "inventory", "stock level refreshed", 33, true),
        LogRow(90006, "INFO", "checkout", "payment authorized", 219, true),
        LogRow(90007, "ERROR", "shipping", "carrier rejected label", 870, false),
        LogRow(90008, "INFO", "reviews", "review published", 41, true),
        LogRow(90009, "WARN", "auth", "password nearing expiry", 18, true),
        LogRow(90010, "INFO", "billing", "invoice generated", 96, true),
    )

// ----- tabular with a nested-uniform column -----

@Serializable data class GeoPoint(val lat: Double, val lon: Double)

@Serializable data class Reading(val sensor: String, val celsius: Double, val at: GeoPoint)

private val READING_SEED =
    listOf(
        Reading("probe north", 21.5, GeoPoint(47.6062, -122.3321)),
        Reading("probe south", 18.25, GeoPoint(29.7604, -95.3698)),
        Reading("probe east", 24.75, GeoPoint(40.7128, -74.006)),
        Reading("probe west", 16.125, GeoPoint(37.7749, -122.4194)),
        Reading("probe central", 22.875, GeoPoint(41.8781, -87.6298)),
        Reading("probe alpine", 4.5, GeoPoint(39.7392, -104.9903)),
        Reading("probe coastal", 19.375, GeoPoint(25.7617, -80.1918)),
        Reading("probe desert", 33.625, GeoPoint(33.4484, -112.074)),
    )

// ----- tabular rejected, list fallback -----

@Serializable
data class Order(val id: Long, val customer: String, val total: Double, val tags: List<String>)

private val ORDER_SEED =
    listOf(
        Order(5001, "acme industries", 1299.5, listOf("priority", "gift wrap")),
        Order(5002, "globex", 84.25, listOf("standard")),
        Order(5003, "initech", 4310.75, listOf("priority", "insured", "signature")),
        Order(5004, "umbrella supply", 219.0, listOf("standard", "fragile")),
        Order(5005, "stark logistics", 15750.25, listOf("priority", "insured")),
        Order(5006, "wayne exports", 640.5, listOf("standard")),
        Order(5007, "tyrell produce", 92.125, listOf("perishable", "expedited")),
        Order(5008, "soylent foods", 1180.0, listOf("standard", "bulk")),
    )

// ----- keyed tabular -----

@Serializable
data class Metrics(val p50: Double, val p95: Double, val p99: Double, val errors: Long)

@Suppress("LongParameterList") // A keyed table needs many uniform entries to be worth measuring.
@Serializable
data class ServiceMetrics(
    val auth: Metrics,
    val billing: Metrics,
    val search: Metrics,
    val cart: Metrics,
    val checkout: Metrics,
    val inventory: Metrics,
    val shipping: Metrics,
    val reviews: Metrics,
    val recommendations: Metrics,
    val notifications: Metrics,
    val analytics: Metrics,
    val gateway: Metrics,
)

private val SERVICES =
    ServiceMetrics(
        auth = Metrics(12.5, 48.25, 96.0, 3),
        billing = Metrics(31.0, 120.5, 310.75, 17),
        search = Metrics(8.125, 22.5, 61.25, 0),
        cart = Metrics(5.5, 19.75, 44.5, 2),
        checkout = Metrics(88.25, 240.0, 512.5, 41),
        inventory = Metrics(14.75, 55.5, 130.25, 6),
        shipping = Metrics(102.5, 388.75, 904.0, 58),
        reviews = Metrics(9.25, 30.5, 78.75, 1),
        recommendations = Metrics(46.0, 175.25, 420.5, 23),
        notifications = Metrics(3.75, 11.5, 28.25, 9),
        analytics = Metrics(210.5, 640.75, 1480.0, 74),
        gateway = Metrics(2.25, 7.75, 18.5, 12),
    )

// ----- wide flat object, streaming path -----

@Suppress("LongParameterList") // The width is the point: this covers the no-capture object path.
@Serializable
data class ServerConfig(
    val host: String,
    val port: Int,
    val workers: Int,
    val backlog: Int,
    val keepAlive: Boolean,
    val tls: Boolean,
    val region: String,
    val zone: String,
    val readTimeoutMs: Long,
    val writeTimeoutMs: Long,
    val idleTimeoutMs: Long,
    val maxRequestBytes: Long,
    val gzip: Boolean,
    val logLevel: String,
    val sampleRate: Double,
    val buildTag: String,
)

private val SERVER_CONFIG =
    ServerConfig(
        host = "edge.internal.example",
        port = 8443,
        workers = 32,
        backlog = 4096,
        keepAlive = true,
        tls = true,
        region = "us west",
        zone = "us west 2b",
        readTimeoutMs = 15000,
        writeTimeoutMs = 15000,
        idleTimeoutMs = 60000,
        maxRequestBytes = 16777216,
        gzip = true,
        logLevel = "INFO",
        sampleRate = 0.125,
        buildTag = "release candidate 4",
    )

// ----- deep nesting -----

@Serializable data class Node(val name: String, val depth: Int, val child: Node?)

private val NODE_NAMES =
    listOf("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel")

// ----- quoting and escaping -----

@Serializable data class Note(val title: String, val body: String, val tag: String)

private val NOTE_SEED =
    listOf(
        Note("say \"hello\" twice", "first line\nsecond line", "12345"),
        Note("path C:\\temp\\out", "column\tseparated\tvalues", "true"),
        Note("key: value", "commas, everywhere, here", " padded "),
        Note("-leading hyphen", "#leading hash", "3.14159"),
        Note("brackets [x] and {y}", "backslash \\ alone", "null"),
        Note("trailing space ", "\ttab first", "0042"),
        Note("quote \" and comma ,", "carriage\rreturn", "false"),
        Note("colon: and bracket ]", "bell \u0007 control", "1e10"),
    )

// ----- number normalization -----

@Serializable
data class Sample(
    val whole: Double,
    val fraction: Double,
    val tiny: Double,
    val huge: Double,
    val ratio: Float,
    val count: Long,
)

private val SAMPLE_SEED =
    listOf(
        Sample(42.0, 3.14159, 0.0000001, 1.23e20, 0.5f, 9007199254740993),
        Sample(-17.0, 2.718281828, 0.00000045, 6.02e23, 1.25f, -4503599627370496),
        Sample(1024.0, 0.1, 0.000000009, 9.99e21, 0.0625f, 123456789012345),
        Sample(0.0, 123.456789, 0.00000012345, 5.5e30, 3.375f, 1),
        Sample(-2048.0, 99.999, 0.0000005, 1.0e100, 0.75f, -1),
        Sample(65536.0, 0.333333333, 0.0000000001, 7.25e18, 2.5f, 8388608),
    )

// ----- inline primitive arrays -----

@Serializable
data class Series(
    val name: String,
    val ticks: List<Long>,
    val values: List<Double>,
    val labels: List<String>,
)

private val TICK_SEED =
    listOf(1700000000L, 1700000060L, 1700000120L, 1700000180L, 1700000240L, 1700000300L)

private val VALUE_SEED = listOf(0.5, 12.25, 88.125, 3.75, 46.5, 71.0, 19.875, 55.25)

private val LABEL_SEED = listOf("idle", "warm", "busy", "peak", "cooling", "steady")
