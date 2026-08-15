package com.lukelast.ktoon.data1.test12

import com.lukelast.ktoon.data1.AbstractGoldenTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Test12: Nested field groups in tabular headers (§9.3)
 *
 * A nested-uniform column is emitted as a field group `name{...}` in the header, and its leaf
 * values are spliced into the flat row in depth-first, pre-order. Covers:
 * - A group beside plain columns: `orders[3]{id,customer{name,country},total}:`
 * - An unbounded single-key chain collapsing to one cell per row: `wrappers[2]{a{b{c}}}:`
 * - Sibling groups: `measurements[2]{p{x,y},q{z}}:`
 * - A nullable primitive inside a group (null is a primitive, so the column stays uniform)
 * - A field name inside a group requiring quotes per §7.3: `person{"first name",last}`
 * - A group cell containing the active delimiter, so the cell is quoted per §7.2
 */
class Test12 : AbstractGoldenTest() {
    override fun verify() = assertGolden(data)
}

@Serializable
data class TabularGroups(
    val orders: List<Order>,
    val wrappers: List<Wrapper>,
    val measurements: List<Measurement>,
    val readings: List<Reading>,
    val contacts: List<Contact>,
    val shipments: List<Shipment>,
)

@Serializable data class Order(val id: Int, val customer: Customer, val total: Double)

@Serializable data class Customer(val name: String, val country: String)

@Serializable data class Wrapper(val a: Alpha)

@Serializable data class Alpha(val b: Beta)

@Serializable data class Beta(val c: Int)

@Serializable data class Measurement(val p: Position, val q: Quality)

@Serializable data class Position(val x: Int, val y: Int)

@Serializable data class Quality(val z: String)

@Serializable data class Reading(val id: Int, val sample: Sample)

/** [note] is null in one row; null is a primitive, so the column stays uniform-primitive. */
@Serializable data class Sample(val value: Double, val note: String?)

@Serializable data class Contact(val id: Int, val person: Person)

@Serializable
data class Person(
    // Space in the name -> quoted inside the nested field group (§7.3).
    @SerialName("first name") val firstName: String,
    val last: String,
)

@Serializable data class Shipment(val id: Int, val origin: Place)

@Serializable data class Place(val city: String, val code: String)

val data =
    TabularGroups(
        orders =
            listOf(
                Order(id = 1, customer = Customer(name = "Ada", country = "DK"), total = 99.5),
                Order(id = 2, customer = Customer(name = "Bob", country = "US"), total = 12.25),
                Order(id = 3, customer = Customer(name = "Cleo", country = "JP"), total = 40.75),
            ),
        wrappers = listOf(Wrapper(Alpha(Beta(c = 1))), Wrapper(Alpha(Beta(c = 2)))),
        measurements =
            listOf(
                Measurement(p = Position(x = 1, y = 2), q = Quality(z = "low")),
                Measurement(p = Position(x = 3, y = 4), q = Quality(z = "high")),
            ),
        readings =
            listOf(
                Reading(id = 1, sample = Sample(value = 21.5, note = "ok")),
                Reading(id = 2, sample = Sample(value = 22.75, note = null)),
            ),
        contacts =
            listOf(
                Contact(id = 1, person = Person(firstName = "Ada", last = "Lovelace")),
                Contact(id = 2, person = Person(firstName = "Grace", last = "Hopper")),
            ),
        shipments =
            listOf(
                // Contains the active delimiter -> quoted cell in the flat row (§7.2).
                Shipment(id = 1, origin = Place(city = "Austin, TX", code = "AUS")),
                Shipment(id = 2, origin = Place(city = "Copenhagen", code = "CPH")),
            ),
    )
