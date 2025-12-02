package com.lukelast.ktoon.data1

import kotlinx.serialization.Serializable

@Serializable
data class Garage(
    val owner: String,
    val location: String,
    val capacity: Int,
    // Tests Section 9.4: Mixed/Non-Uniform/Complex Arrays (Expanded List)
    val inventory: List<SportsCar>,
)

@Serializable
data class SportsCar(
    val vin: String,
    val make: String,
    val model: String,
    val year: Int,
    val isStreetLegal: Boolean,

    // Tests Section 8: Nested Objects
    val engineSpec: EngineSpec,

    // Tests Section 9.1: Primitive Arrays (Inline)
    // Expectation: features[N]: Turbo,AWD,Carbon
    val features: List<String>,

    // Tests Section 9.3: Arrays of Objects (Tabular Form)
    // These objects contain ONLY primitives and share the same keys.
    // Expectation: lapTimes[N]{track,seconds}:
    val lapTimes: List<LapTime>,

    // Tests Section 9.4: Arrays of Objects (Expanded List)
    // This cannot be tabular because it contains a nullable field and potentially nested lists.
    val modifications: List<Modification>,
)

@Serializable
data class EngineSpec(
    val type: String,
    val displacement: Double, // Tests Canonical Number formatting (Section 2)
    val horsepower: Int,
    val torque: Float,
)

/** Designed for Tabular Array testing (Section 9.3). Uniform keys, primitive values only. */
@Serializable data class LapTime(val track: String, val seconds: Double)

/**
 * Designed for Expanded List testing (Section 9.4 / 10). Contains a nullable field to test `null`
 * serialization.
 */
@Serializable
data class Modification(
    val name: String,
    val cost: Double?, // Nullable: should encode as literal `null` if missing
    val dateInstalled: String,
)
