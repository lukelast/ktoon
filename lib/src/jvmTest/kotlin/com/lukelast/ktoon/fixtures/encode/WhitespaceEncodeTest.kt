package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

/** Tests from whitespace.json fixture - Whitespace and formatting invariants. */
class WhitespaceEncodeTest {

    private val fixture = "whitespace"

    @Test
    fun `produces no trailing newline at end of output`() {
        @Serializable data class Id(val id: Int)

        runFixtureEncodeTest<Id>(fixture)
    }

    @Test
    fun `maintains proper indentation for nested structures`() {
        @Serializable data class User(val id: Int, val name: String)

        @Serializable data class Root(val user: User, val items: List<String>)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `respects custom indent size option`() {
        @Serializable data class User(val name: String, val role: String)

        @Serializable data class Root(val user: User)

        runFixtureEncodeTest<Root>(fixture)
    }
}
