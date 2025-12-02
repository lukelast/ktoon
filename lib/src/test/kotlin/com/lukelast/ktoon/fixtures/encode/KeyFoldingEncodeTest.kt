package com.lukelast.ktoon.fixtures.encode

import com.lukelast.ktoon.fixtures.runFixtureEncodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from key-folding.json fixture - Key folding with safe mode, depth control, collision
 * avoidance.
 */
class KeyFoldingEncodeTest {

    private val fixture = "key-folding"

    @Test
    fun `encodes folded chain to primitive (safe mode)`() {
        @Serializable data class C(val c: Int)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes folded chain with inline array`() {
        @Serializable data class Meta(val items: List<String>)

        @Serializable data class Data(val meta: Meta)

        @Serializable data class Root(val data: Data)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes folded chain with tabular array`() {
        @Serializable data class Item(val id: Int, val name: String)

        @Serializable data class B(val items: List<Item>)

        @Serializable data class A(val b: B)

        @Serializable data class Root(val a: A)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `skips folding when segment requires quotes (safe mode)`() {
        @Serializable data class FullName(val x: Int)

        @Serializable data class Data(@SerialName("full-name") val fullName: FullName)

        @Serializable data class Root(val data: Data)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `skips folding on sibling literal-key collision (safe mode)`() {
        @Serializable data class Meta(val items: List<Int>)

        @Serializable data class Data(val meta: Meta)

        @Serializable
        data class Root(val data: Data, @SerialName("data.meta.items") val dataMetaItems: String)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes partial folding with flattenDepth=2`() {
        @Serializable data class D(val d: Int)

        @Serializable data class C(val c: D)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes full chain with flattenDepth=Infinity (default)`() {
        @Serializable data class D(val d: Int)

        @Serializable data class C(val c: D)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes standard nesting with flattenDepth=0 (no folding)`() {
        @Serializable data class C(val c: Int)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes standard nesting with flattenDepth=1 (no practical effect)`() {
        @Serializable data class C(val c: Int)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes standard nesting with keyFolding=off (baseline)`() {
        @Serializable data class C(val c: Int)

        @Serializable data class B(val b: C)

        @Serializable data class Root(val a: B)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes folded chain ending with empty object`() {
        @Serializable data class C(val d: Unit? = null)
        @Serializable data class B(val c: C)

        @Serializable data class A(val b: B)

        @Serializable data class Root(val a: A)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `stops folding at array boundary (not single-key object)`() {
        @Serializable data class A(val b: List<Int>)

        @Serializable data class Root(val a: A)

        runFixtureEncodeTest<Root>(fixture)
    }

    @Test
    fun `encodes folded chains preserving sibling field order`() {

        @Serializable data class Second(val third: Int)

        @Serializable data class First(val second: Second)

        @Serializable data class Short(val path: Int)

        @Serializable data class Root(val first: First, val simple: Int, val short: Short)

        runFixtureEncodeTest<Root>(fixture)
    }
}
