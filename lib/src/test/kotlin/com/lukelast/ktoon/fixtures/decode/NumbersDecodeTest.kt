package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import com.lukelast.ktoon.util.isDigit
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * Tests from numbers.json fixture - Number decoding edge cases: trailing zeros, exponent forms,
 * negative zero.
 */
class NumbersDecodeTest {

    private val fixture = "numbers"

    @Test
    fun `parses number with trailing zeros in fractional part`() {
        @Serializable data class Value(val value: Double)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses negative number with positive exponent`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses lowercase exponent`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses uppercase exponent with negative sign`() {
        @Serializable data class Value(val value: Double)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses negative zero as zero`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses negative zero with fractional part`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses array with mixed numeric forms`() {
        // Note: all numbers are getting parsed to doubles.
        @Serializable data class Nums(val nums: List<Double>)
        runFixtureDecodeTest<Nums>(fixture)
    }

    @Test
    fun `treats leading zero as string not number`() {
        @Serializable data class Value(val value: String)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses very small exponent`() {
        @Serializable data class Value(val value: Double)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses integer with positive exponent`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses exponent notation`() {
        runFixtureDecodeTest<Int>(fixture)
    }

    @Test
    fun `parses exponent notation with uppercase E`() {
        runFixtureDecodeTest<Int>(fixture)
    }

    @Test
    fun `parses negative exponent notation`() {
        runFixtureDecodeTest<Double>(fixture)
    }

    @Test
    fun `treats unquoted leading-zero number as string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `treats unquoted multi-leading-zero as string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `treats unquoted octal-like as string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `treats leading-zero in object value as string`() {
        @Serializable data class A(val a: String)
        runFixtureDecodeTest<A>(fixture)
    }

    @Test
    fun `treats leading-zeros in array as strings`() {
        @Serializable data class Nums(val nums: List<String>)
        runFixtureDecodeTest<Nums>(fixture)
    }

    @Test
    fun `parses zero with exponent as number`() {
        @Serializable data class Value(val value: Int)
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `parses negative zero with exponent as number`() {
        @Serializable data class Value(val value: Int)
        '0'.isDigit()
        runFixtureDecodeTest<Value>(fixture)
    }

    @Test
    fun `treats unquoted negative leading-zero number as string`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Test
    fun `treats negative leading-zeros in array as strings`() {
        @Serializable data class Nums(val nums: List<String>)
        runFixtureDecodeTest<Nums>(fixture)
    }
}
