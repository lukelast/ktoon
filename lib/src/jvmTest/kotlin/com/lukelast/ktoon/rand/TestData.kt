package com.lukelast.ktoon.rand

import kotlinx.serialization.Serializable

@Serializable
data class FarmTestData(
    val primitives: PrimitiveBarn,
    val nullablePrimitives: NullableCorral,
    val collections: CollectionCoop,
    val nesting: NestingPaddock,
    val generics: GenericStable,
)

@Serializable
data class PrimitiveBarn(
    val barnId: Int,
    val hasHay: Boolean,
    val hayBales: Long,
    val milkLiters: Double,
    val rainGauge: Float,
    val roosterCode: Char,
    val pigletByte: Byte,
    val lambShort: Short,
    val farmerName: String,
    val irrigationCount: UInt,
    val fenceBoards: UShort,
    val cornBins: UByte,
    val siloCapacity: ULong,
)

@Serializable
data class NullableCorral(
    val barnId: Int?,
    val hasHay: Boolean?,
    val hayBales: Long?,
    val milkLiters: Double?,
    val rainGauge: Float?,
    val roosterCode: Char?,
    val pigletByte: Byte?,
    val lambShort: Short?,
    val farmerName: String?,
    val irrigationCount: UInt?,
    val fenceBoards: UShort?,
    val cornBins: UByte?,
    val siloCapacity: ULong?,
)

@Serializable
data class CollectionCoop(
    val goatNames: List<String>,
    val barnAnimals: Set<FarmAnimal>,
    val feedByAnimal: Map<String, Int>,
    val choresByDay: Map<String, List<String>>,
    val animalsByPen: Map<Int, List<FarmAnimal>>,
    val nestedHerds: List<Set<FarmAnimal>>,
    val animalPairs: List<Pair<String, FarmAnimal>>,
    val taggedTrios: Set<Triple<String, FarmAnimal, Boolean>>,
    val hayPiles: List<List<Int>>,
    val pastureGrid: List<List<List<String>>>,
    val nullableAnimals: List<FarmAnimal?>,
    val nullableFeedPerPen: Map<String, List<Double>?>,
    val mixedFeedCrates: Map<String, FeedCrate<FeedMix>>,
    val animalStacks: Map<String, Map<String, List<FarmAnimal>>>,
)

@Serializable
data class NestingPaddock(
    val farmstead: Farmstead,
    val orchard: Orchard?,
    val duckPond: DuckPond,
    val winterPlan: SeasonalPlan?,
    val backupHerds: List<Herd>,
    val animalLineage: Map<String, Lineage>,
    val caretakerNotes: Map<String, List<Note?>>,
)

@Serializable data class Farmstead(val barn: Barn, val pasture: Pasture, val farmhouse: FarmHouse)

@Serializable
data class Barn(
    val name: String,
    val herd: Herd,
    val storage: HayStack?,
    val notes: Map<String, String>,
)

@Serializable data class Herd(val lead: FarmAnimal, val followers: List<FarmAnimal>)

@Serializable data class HayStack(val baleCount: Int, val dryness: Float, val lastCovered: Boolean?)

@Serializable
data class Pasture(val acres: Double, val fences: FenceLine, val shelters: List<Shelter>)

@Serializable
data class FenceLine(val posts: List<Int>, val gateOpen: Boolean, val electrified: Boolean?)

@Serializable
data class Shelter(
    val type: String,
    val capacity: Short,
    val animals: List<FarmAnimal?>,
    val bedding: FeedMix?,
)

@Serializable
data class FarmHouse(
    val familyName: String,
    val residents: List<String>,
    val utilities: Map<String, Boolean>,
)

@Serializable
data class DuckPond(
    val duckNames: Set<String>,
    val depths: List<Float>,
    val hasFountain: Boolean,
    val fishSpecies: List<String>?,
)

@Serializable
data class Orchard(
    val fruitTrees: Map<String, Int>,
    val scarecrows: List<Scarecrow>,
    val irrigationSchedule: Map<String, List<Int>>,
)

@Serializable data class Scarecrow(val name: String, val hatColor: String?, val guardRadius: Double)

@Serializable
data class SeasonalPlan(
    val tasks: List<String>,
    val livestockMoves: Map<String, PastureMove>,
    val backupShelters: List<Shelter?>,
    val emergencyContacts: Set<String>,
)

@Serializable
data class PastureMove(val from: String, val to: String, val animals: List<FarmAnimal>)

@Serializable
data class Lineage(
    val mother: Ancestor?,
    val father: Ancestor?,
    val originFarm: String?,
    val traits: Temperament,
    val notes: List<String>,
)

@Serializable
data class Ancestor(
    val awards: List<String>,
    val breed: String,
    val name: String,
    val tags: Set<Long>,
)

@Serializable data class Note(val author: String, val message: String?, val priority: Int)

@Serializable
data class FarmAnimal(
    val tag: Long,
    val name: String,
    val species: String,
    val ageYears: Int,
    val weightKg: Double,
    val favoriteTreats: List<String>,
    val isVaccinated: Boolean,
    val temperament: Temperament,
    val birthPasture: String?,
    val waterIntakeLiters: Float,
    val nightBarn: Char,
    val microchip: ULong?,
    val woolQuality: UByte?,
    val siblingWeights: List<Short>,
    val earNotches: Set<Byte>,
    val lineage: Lineage?,
)

@Serializable
data class Temperament(
    val score: UInt,
    val isSkittish: Boolean,
    val herdFriendly: Boolean?,
    val moodTags: Set<String>,
)

@Serializable
data class GenericStable(
    val alfalfaSack: FeedCrate<FeedMix>,
    val beddingBundle: FeedCrate<List<String>>,
    val medicalCrate: FeedCrate<Map<String, FarmAnimal?>>,
    val pairedCrate: FeedCrate<Pair<FarmAnimal, FarmAnimal>>,
    val nestedCrate: FeedCrate<FeedCrate<FeedMix>>,
    val labeledFeed: Pair<String, FeedCrate<FeedMix>>,
)

@Serializable data class FeedCrate<T>(val label: String, val contents: T)

@Serializable
data class FeedMix(val grains: List<String>, val protein: Double, val hasMolasses: Boolean)
