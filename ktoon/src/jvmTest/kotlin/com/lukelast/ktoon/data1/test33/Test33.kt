package com.lukelast.ktoon.data1.test33

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Test33: Default values and overrides Tests encoding/decoding when properties have defaults:
 * - Default values are omitted when not overridden
 * - Non-default values are encoded explicitly Expected: Decoding restores defaults for missing
 *   fields
 */
class Test33 : Runner() {
    override fun run() = doTest(data)
}
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DefaultsData(
    val title: String = "Default Title",
    val name: String = "Default Name",
    val description: String = "Default Description",
    val defaultNullSetToNull: Int? = null,
    val defaultNullSetToOne: Int? = null,
    val defaultOneSetToNull: Int? = 1,
    val defaultOneSetToOne: Int? = 1,
    @EncodeDefault(Mode.NEVER)
    val defaultNullSetToNullModeNever: Int? = null,
    @EncodeDefault(Mode.NEVER)
    val defaultNullSetToOneModeNever: Int? = null,
    @EncodeDefault(Mode.NEVER)
    val defaultOneSetToNullModeNever: Int? = 1,
    @EncodeDefault(Mode.NEVER)
    val defaultOneSetToOneModeNever: Int? = 1,
    @EncodeDefault(Mode.ALWAYS)
    val defaultNullSetToNullModeAlways: Int? = null,
    @EncodeDefault(Mode.ALWAYS)
    val defaultNullSetToOneModeAlways: Int? = null,
    @EncodeDefault(Mode.ALWAYS)
    val defaultOneSetToNullModeAlways: Int? = 1,
    @EncodeDefault(Mode.ALWAYS)
    val defaultOneSetToOneModeAlways: Int? = 1,
    val owner: Owner = Owner(),
    val members: List<Member> = emptyList(),
)

@Serializable
data class Owner(val name: String = "system", val email: String? = null, val team: String? = "core")

@Serializable
data class Member(
    val id: Int,
    val role: String = "viewer",
    val active: Boolean = true,
)

val data =
    DefaultsData(
        title = "Non Default Title",
        description = "Default Description",
        defaultNullSetToNull = null,
        defaultNullSetToOne = 1,
        defaultOneSetToNull = null,
        defaultOneSetToOne = 1,
        defaultNullSetToNullModeNever = null,
        defaultNullSetToOneModeNever = 1,
        defaultOneSetToNullModeNever = null,
        defaultOneSetToOneModeNever = 1,
        defaultNullSetToNullModeAlways = null,
        defaultNullSetToOneModeAlways = 1,
        defaultOneSetToNullModeAlways = null,
        defaultOneSetToOneModeAlways = 1,
        members =
            listOf(
                Member(id = 1),
                Member(id = 2, role = "admin", active = false),
            ),
    )
