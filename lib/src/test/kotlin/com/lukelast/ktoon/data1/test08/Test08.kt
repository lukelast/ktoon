package com.lukelast.ktoon.data1.test08

import com.lukelast.ktoon.data1.*

/**
 * Edge cases with single-item arrays. Tests single-item inline arrays, single-row tabular arrays,
 * and single-item expanded lists.
 */
class Test08 : Runner() {
    override fun run() = doTest(data)
}

private val data =
    Garage(
        owner = "Frank",
        location = "Driveway",
        capacity = 1,
        inventory =
            listOf(
                SportsCar(
                    vin = "PORS718GT4",
                    make = "Porsche",
                    model = "718 Cayman GT4",
                    year = 2023,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "Flat-6 NA",
                            displacement = 4.0,
                            horsepower = 414,
                            torque = 309f,
                        ),
                    // Tests Inline Primitive Array of length 1 (No delimiters needed)
                    features = listOf("Bucket Seats"),

                    // Tests Tabular Array of length 1 (Header + Single Row)
                    lapTimes = listOf(LapTime(track = "Hockenheim", seconds = 108.5)),

                    // Tests Expanded List of length 1
                    modifications =
                        listOf(
                            Modification(
                                name = "Brake Pads",
                                cost = 600.0,
                                dateInstalled = "2023-05-20",
                            )
                        ),
                )
            ),
    )
