package com.lukelast.ktoon.data1.test34

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

/**
 * Test34: Keyed tabular form at the document root (§9.5 root position). The root value is itself an
 * object of uniform objects, so the header drops the key and the whole document is
 * `[2:]{code,users,active}:` followed by one entry row per key (§6, §5).
 */
class Test34 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

@Serializable data class Region(val code: String, val users: Int, val active: Boolean)

val data =
    mapOf(
        "north" to Region(code = "us-n", users = 120, active = true),
        "south" to Region(code = "us-s", users = 45, active = false),
    )
