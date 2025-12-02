package com.lukelast.ktoon.data1.test07

import com.lukelast.ktoon.data1.*

/**
 * Heavy testing with 3 sports cars. Tests large tabular arrays, mixed nulls, long primitive lists,
 * and empty lists.
 */
class Test07 : Runner() {
    override fun run() = doTest(data)
}

private val data =
    Garage(
        owner = "Luke",
        location = "Jay's Warehouse",
        capacity = 20,
        inventory =
            listOf(
                // Car 1: Tests heavy Tabular Array usage (4 lap times)
                SportsCar(
                    vin = "MCL765LT001",
                    make = "McLaren",
                    model = "765LT",
                    year = 2021,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "V8 Twin-Turbo",
                            displacement = 4.0,
                            horsepower = 755,
                            torque = 590f,
                        ),
                    features =
                        listOf("Carbon Tub", "Active Aero", "Titanium Exhaust", "Roof Scoop"),
                    lapTimes =
                        listOf(
                            LapTime(track = "Silverstone", seconds = 105.4),
                            LapTime(track = "Spa Francorchamps", seconds = 143.2),
                            LapTime(track = "Monza", seconds = 109.8),
                            LapTime(track = "Nurburgring", seconds = 415.0),
                        ),
                    modifications =
                        listOf(
                            Modification(
                                name = "Slicks",
                                cost = 4500.0,
                                dateInstalled = "2021-06-01",
                            ),
                            Modification(
                                name = "Telemetry Kit",
                                cost = 1200.0,
                                dateInstalled = "2021-06-05",
                            ),
                        ),
                ),

                // Car 2: Tests heavy Expanded List usage with mixed Nulls (3 mods)
                SportsCar(
                    vin = "LAMBOSTO123",
                    make = "Lamborghini",
                    model = "Huracan STO",
                    year = 2022,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "V10 NA",
                            displacement = 5.2,
                            horsepower = 630,
                            torque = 417f,
                        ),
                    features = listOf("RWD", "CCMR Brakes", "Cofango"),
                    lapTimes =
                        listOf(
                            LapTime(track = "Daytona", seconds = 112.5) // Single row tabular check
                        ),
                    modifications =
                        listOf(
                            Modification(
                                name = "Full PPF",
                                cost = 6000.0,
                                dateInstalled = "2022-02-01",
                            ),
                            Modification(
                                name = "Warranty Work",
                                cost = null,
                                dateInstalled = "2022-08-15",
                            ),
                            Modification(
                                name = "Titanium Bolts",
                                cost = 800.0,
                                dateInstalled = "2022-09-20",
                            ),
                        ),
                ),

                // Car 3: Tests primitive array wrapping and empty lists
                SportsCar(
                    vin = "DODGEACR999",
                    make = "Dodge",
                    model = "Viper ACR",
                    year = 2017,
                    isStreetLegal = true,
                    engineSpec =
                        EngineSpec(
                            type = "V10 NA",
                            displacement = 8.4,
                            horsepower = 645,
                            torque = 600f,
                        ),
                    // Long primitive list
                    features =
                        listOf(
                            "Manual",
                            "Extreme Aero",
                            "Adjustable Suspension",
                            "Kumho Tires",
                            "No Radio",
                            "No Sound Deadening",
                            "Lightweight Battery",
                            "Track Cooling Package",
                        ),
                    lapTimes =
                        listOf(
                            LapTime(track = "Laguna Seca", seconds = 88.6),
                            LapTime(track = "Road Atlanta", seconds = 86.3),
                            LapTime(track = "VIR", seconds = 158.4),
                        ),
                    modifications = emptyList(), // Empty expanded list check
                ),
            ),
    )
