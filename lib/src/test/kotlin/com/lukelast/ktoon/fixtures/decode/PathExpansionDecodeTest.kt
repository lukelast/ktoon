package com.lukelast.ktoon.fixtures.decode

import com.lukelast.ktoon.fixtures.runFixtureDecodeTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests from path-expansion.json fixture - Path expansion with safe mode, deep merge, conflict
 * resolution tied to strict mode.
 */
class PathExpansionDecodeTest {

    private val fixture = "path-expansion"

    @Serializable data class DeepNested(val a: NestedB)

    @Serializable data class NestedB(val b: NestedC)

    @Serializable data class NestedC(val c: Int)

    @Test
    fun `expands dotted key to nested object in safe mode`() {
        runFixtureDecodeTest<DeepNested>(fixture)
    }

    @Serializable data class DeepData(val data: DataMeta)

    @Serializable data class DataMeta(val meta: MetaItems)

    @Serializable data class MetaItems(val items: List<String>)

    @Test
    fun `expands dotted key with inline array`() {
        runFixtureDecodeTest<DeepData>(fixture)
    }

    @Serializable data class Item(val id: Int, val name: String)

    @Serializable data class DeepTabular(val a: ALevel)

    @Serializable data class ALevel(val b: BLevel)

    @Serializable data class BLevel(val items: List<Item>)

    @Test
    fun `expands dotted key with tabular array`() {
        runFixtureDecodeTest<DeepTabular>(fixture)
    }

    @Test
    fun `preserves literal dotted keys when expansion is off`() {
        runFixtureDecodeTest<Map<String, String>>(fixture)
    }

    @Serializable data class DeepMerge(val a: AMerge)

    @Serializable data class AMerge(val b: BMerge, val e: Int)

    @Serializable data class BMerge(val c: Int, val d: Int)

    @Test
    fun `expands and deep-merges preserving document-order insertion`() {
        runFixtureDecodeTest<DeepMerge>(fixture)
    }

    @Test
    fun `throws on expansion conflict (object vs primitive) when strict=true`() {
        runFixtureDecodeTest<Map<String, String>>(fixture)
    }

    @Test
    fun `throws on expansion conflict (object vs array) when strict=true`() {
        runFixtureDecodeTest<String>(fixture)
    }

    @Serializable data class LWWPrimitive(val a: Int)

    @Test
    fun `applies LWW when strict=false (primitive overwrites expanded object)`() {
        runFixtureDecodeTest<LWWPrimitive>(fixture)
    }

    @Serializable data class NestedWithB(val b: Int)

    @Serializable data class LWWObject(val a: NestedWithB)

    @Test
    fun `applies LWW when strict=false (expanded object overwrites primitive)`() {
        runFixtureDecodeTest<LWWObject>(fixture)
    }

    @Serializable data class PreserveQuoted(val a: Map<String, Int>, @SerialName("c.d") val cd: Int)

    @Test
    fun `preserves quoted dotted key as literal when expandPaths=safe`() {
        runFixtureDecodeTest<PreserveQuoted>(fixture)
    }

    @Test
    fun `preserves non-IdentifierSegment keys as literals`() {
        runFixtureDecodeTest<Map<String, Int>>(fixture)
    }

    @Serializable data class EmptyNested(val a: AEmpty)

    @Serializable data class AEmpty(val b: BEmpty)

    @Serializable data class BEmpty(val c: Map<String, String>)

    @Test
    fun `expands keys creating empty nested objects`() {
        runFixtureDecodeTest<EmptyNested>(fixture)
    }
}
