package com.lukelast.ktoon.data1.test13

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.Serializable

/**
 * Test13: Empty-object spellings and list-item edge forms.
 *
 * Kotlin data classes cannot be empty, so `Map<String, Int>` stands in for the JSON empty object
 * `{}`; maps also round-trip through `equals`.
 * - §8 – an empty object is a bare `key:` line, never `key: []`.
 * - §9.1 – an empty array in field position is `key: []`, never a `[0]` header.
 * - §10 – an empty object as a list item is a bare `-` marker (§9.3 forbids tabular form for arrays
 *   containing `{}`).
 * - §9.2 – an empty *inner* array in an array of arrays is `- [0]:`; this is the only place a `[0`
 *   header is emitted.
 * - §9.4/§10 – objects with differing key sets stay in list form with the first field carried on
 *   the hyphen line; when that first field is an empty object the remaining fields sit at +1 under
 *   a bare `- key:`.
 * - §9.5 (negative) – an object of non-uniform / empty objects MUST NOT collapse into a keyed
 *   tabular `[N:]` header; it stays plainly nested.
 */
class Test13 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

/** First field is an empty object in one element, which also disqualifies tabular form (§9.3). */
@Serializable data class EmptyFirstFieldItem(val meta: Map<String, Int>, val id: Int)

@Serializable
data class EmptyObjectFormsData(
    /** §8: bare `emptyObject:` line. */
    val emptyObject: Map<String, Int>,
    /** §9.1: `emptyArray: []` – contrast with the bare line above. */
    val emptyArray: List<Int>,
    /** §10: `[2]:` header followed by two bare `-` markers. */
    val emptyObjectList: List<Map<String, Int>>,
    /** §9.2: `- [0]:` for the empty inner array, `- [2]: 1,2` for the other. */
    val arrayOfArrays: List<List<Int>>,
    /** §9.4/§10: differing key sets keep list form, first field on the hyphen line. */
    val nonUniformObjects: List<Map<String, Int>>,
    /** §10: `- meta:` with `id` at +1 when the first field is an empty object. */
    val emptyFirstField: List<EmptyFirstFieldItem>,
    /** §9.5 negative: heterogeneous inner key sets, so plain nesting and a bare `beta:`. */
    val mapOfMaps: Map<String, Map<String, Int>>,
)

val data =
    EmptyObjectFormsData(
        emptyObject = emptyMap(),
        emptyArray = emptyList(),
        emptyObjectList = listOf(emptyMap(), emptyMap()),
        arrayOfArrays = listOf(emptyList(), listOf(1, 2)),
        nonUniformObjects = listOf(mapOf("id" to 1), mapOf("id" to 2, "extra" to 3)),
        emptyFirstField =
            listOf(
                EmptyFirstFieldItem(meta = emptyMap(), id = 1),
                EmptyFirstFieldItem(meta = mapOf("weight" to 7), id = 2),
            ),
        mapOfMaps =
            mapOf(
                "alpha" to mapOf("x" to 1),
                "beta" to emptyMap(),
                "gamma" to mapOf("y" to 2, "z" to 3),
            ),
    )
