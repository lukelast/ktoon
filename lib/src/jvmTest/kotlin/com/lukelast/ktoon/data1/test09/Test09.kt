package com.lukelast.ktoon.data1.test09

import com.lukelast.ktoon.data1.*

/**
 * Special character and quoting tests. Tests quoted strings, delimiter handling, Unicode, null-like
 * strings, and negative zero.
 */
class Test09 : Runner() {
    override fun run() = doTest(data)
}

private val data =
    Garage(
        // Tests: Quoted strings containing quotes and colons
        owner = "Mr. \"Robot\" : Admin",

        // Tests: String containing the document delimiter (comma) -> Must be quoted
        location = "Area 51, Nevada",

        // Tests: Zero handling
        capacity = 0,
        inventory =
            listOf(
                SportsCar(
                    // Tests: String that looks like a literal null -> Must be quoted "null"
                    vin = "null",

                    // Tests: String that looks like a boolean -> Must be quoted "true"
                    make = "true",

                    // Tests: String that looks like a number -> Must be quoted "911"
                    model = "911",

                    // Tests: Negative zero (should normalize to 0 per Spec §2)
                    year = -0,
                    isStreetLegal = false,
                    engineSpec =
                        EngineSpec(
                            type = "", // Tests: Empty string -> Must be quoted ""
                            displacement =
                                0.000001, // Tests: Canonical decimal format (no Scientific
                            // notation)
                            horsepower = -100,
                            torque = 50.12345f,
                        ),

                    // Tests: Inline Array with special characters (Spec §7.2)
                    // 1. "," contains delimiter -> ","
                    // 2. ":" contains colon -> ":"
                    // 3. "[" contains bracket -> "["
                    // 4. " " contains space (trimmed by default, so usually quoted if intended as
                    // value)
                    // 5. "-" starts with hyphen -> "-"
                    features = listOf(",", ":", "[", " ", "-"),

                    // Tests: Tabular Array with value containing active delimiter
                    lapTimes =
                        listOf(
                            LapTime(
                                track = "Track, A",
                                seconds = 60.0,
                            ), // "Track, A" must be quoted inside the row
                            LapTime(track = "Track B", seconds = 120.5),
                        ),
                    modifications =
                        listOf(
                            Modification(
                                // Tests: Starts with hyphen -> Must be quoted "- Negative Camber"
                                // Otherwise it looks like a list item marker.
                                name = "- Negative Camber",

                                // Tests: Null value in expanded list
                                cost = null,

                                // Tests: Unicode characters -> Should be safe unquoted usually,
                                // unless they contain delimiters.
                                dateInstalled = "2025-01-01 🏁",
                            )
                        ),
                )
            ),
    )
