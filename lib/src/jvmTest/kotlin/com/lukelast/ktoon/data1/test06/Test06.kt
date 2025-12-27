package com.lukelast.ktoon.data1.test06

import com.lukelast.ktoon.data1.*

/**
 * Basic Garage test with 2 sports cars. Tests tabular arrays, nested objects, null values, and
 * empty arrays.
 */
class Test06 : Runner() {
    override fun run() = doTest(data)
}

private val data =
    Garage(
        owner = "Luke",
        location = "Dream Garage",
        capacity = 5,
        inventory =
            listOf(
                SportsCar(
                    vin = "PORS911GT3",
                    make = "Porsche",
                    model = "911 GT3",
                    year = 2024,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "Flat-6",
                            displacement = 4.0,
                            horsepower = 502,
                            torque = 346f,
                        ),
                    features = listOf("PCCB", "Cage", "Wing"),
                    lapTimes =
                        listOf(
                            LapTime(track = "Nurburgring", seconds = 415.5),
                            LapTime(track = "Laguna Seca", seconds = 90.2),
                            LapTime(track = "Silverstone", seconds = 112.7),
                            LapTime(track = "Spa-Francorchamps", seconds = 147.1),
                            LapTime(track = "Monza", seconds = 110.3),
                        ),
                    modifications =
                        listOf(
                            Modification(
                                name = "Exhaust",
                                cost = 5000.0,
                                dateInstalled = "2024-01-10",
                            ),
                            Modification(
                                name = "Decals",
                                cost = null, // Tests null literal encoding
                                dateInstalled = "2024-01-15",
                            ),
                        ),
                ),
                SportsCar(
                    vin = "FERR296GTB",
                    make = "Ferrari",
                    model = "296 GTB",
                    year = 2023,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "V6 Hybrid",
                            displacement = 3.0,
                            horsepower = 819,
                            torque = 546f,
                        ),
                    features = listOf("Hybrid", "Active Aero"),
                    lapTimes = emptyList(), // Tests empty tabular array: lapTimes[0]{...}:
                    modifications = emptyList(), // Tests empty list array: modifications[0]:
                ),
            ),
    )
