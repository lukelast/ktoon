package com.lukelast.ktoon.data1.test03

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

/**
 * Test03: Keyed tabular form in field position (§9.5) with the negative controls that keep an
 * object in nested form (§8).
 * - `metrics`: three uniform entries collapse to `metrics[3:]{count,label,active}:` with `entrykey:
 *   cell,cell,cell` rows.
 * - `metrics.mem.label` holds a comma, so that cell is quoted inside its entry row (§7.2).
 * - `servers`: a nested-uniform column becomes a nested field group,
 *   `servers[2:]{region,limits{cpu,mem}}:`; its entry keys `web-01` and `db 2` fail the unquoted
 *   key pattern and MUST be quoted in the rows (§7.3).
 * - `solo`: a single entry never qualifies (detection needs at least two), so it nests instead of
 *   emitting `[1:]`.
 * - `counts`: primitive values are not objects, so it stays plain `key: value` lines.
 */
class Test03 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

@Serializable data class Metric(val count: Int, val label: String, val active: Boolean)

@Serializable data class Limits(val cpu: Int, val mem: Int)

@Serializable data class Server(val region: String, val limits: Limits)

@Serializable
data class KeyedTabularData(
    val metrics: Map<String, Metric>,
    val servers: Map<String, Server>,
    val solo: Map<String, Metric>,
    val counts: Map<String, Int>,
)

val data =
    KeyedTabularData(
        metrics =
            mapOf(
                "cpu" to Metric(count = 12, label = "CPU load", active = true),
                "mem" to Metric(count = 48, label = "Memory, resident", active = false),
                "disk" to Metric(count = 7, label = "Disk io", active = true),
            ),
        servers =
            mapOf(
                "web-01" to Server(region = "us-east", limits = Limits(cpu = 2, mem = 4096)),
                "db 2" to Server(region = "eu-west", limits = Limits(cpu = 8, mem = 16384)),
            ),
        solo = mapOf("only" to Metric(count = 1, label = "single", active = true)),
        counts = mapOf("alpha" to 1, "beta" to 2, "gamma" to 3),
    )
