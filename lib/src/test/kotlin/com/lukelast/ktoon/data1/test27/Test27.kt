package com.lukelast.ktoon.data1.test27

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Test27: Keys with dots as literals (§7.3, §8, §13.4)
 * Tests that dotted keys are treated as literal keys when path expansion is disabled (default)
 * Unquoted keys can contain dots per §7.3: ^[A-Za-z_][A-Za-z0-9_.]*$
 * Expected: Keys remain as literals, not expanded into nested objects
 */
class Test27 : Runner() {
    override fun run() = doTest(data)
}

@Serializable
data class DottedKeysData(
    @SerialName("file.name")
    val fileName: String,

    @SerialName("version.1.0")
    val version: String,

    @SerialName("a.b.c")
    val abc: String,

    @SerialName("config.setting.value")
    val configSetting: String,

    @SerialName("user.profile.email")
    val userEmail: String,

    // Mix with regular keys
    val normalKey: String,

    @SerialName("data.items")
    val dataItems: List<Int>
)

val data = DottedKeysData(
    fileName = "document.txt",
    version = "1.0.0",
    abc = "nested path",
    configSetting = "enabled",
    userEmail = "user@example.com",
    normalKey = "regular value",
    dataItems = listOf(1, 2, 3)
)
