package com.lukelast.ktoon.fixtures.test

import com.lukelast.ktoon.fixtures.runFixtureTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from whitespace.json fixture - Whitespace and formatting invariants.
 */
class WhitespaceEncodeTest {

    private val fixture = "whitespace"

    @Test
    fun `produces no trailing newline at end of output`() {
        @Serializable
        data class Id(val id: Int)

        runFixtureTest<Id>(fixture, "produces no trailing newline at end of output")
    }

    @Test
    fun `maintains proper indentation for nested structures`() {
        @Serializable
        data class User(val id: Int, val name: String)

        @Serializable
        data class Root(val user: User, val items: List<String>)

        runFixtureTest<Root>(fixture, "maintains proper indentation for nested structures")
    }

    @Test
    fun `respects custom indent size option`() {
        @Serializable
        data class User(val name: String, val role: String)

        @Serializable
        data class Root(val user: User)

        runFixtureTest<Root>(fixture, "respects custom indent size option")
    }
}
