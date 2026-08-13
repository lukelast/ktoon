package com.lukelast.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Shape {
    @Serializable @SerialName("circle") data class Circle(val r: Int) : Shape

    @Serializable @SerialName("square") data class Square(val side: Int) : Shape
}

interface Note

@Serializable @SerialName("memo") data class Memo(val text: String) : Note

/** Regression tests for polymorphic values reported in `.workflow/issues`. */
class IssuePolymorphicTest {

    private val ktoon = Ktoon()

    @Serializable data class Holder(val name: String, val shape: Shape)

    @Test
    fun `a sealed value keeps its own object in a field`() {
        val value = Holder("a", Shape.Circle(3))
        val expected = "name: a\nshape:\n  type: circle\n  value:\n    r: 3"
        assertEquals(expected, ktoon.encodeToString(value))
        assertEquals(value, ktoon.decodeFromString<Holder>(expected))
    }

    @Test
    fun `a sealed value round-trips at the root`() {
        val value: Shape = Shape.Square(2)
        val expected = "type: square\nvalue:\n  side: 2"
        assertEquals(expected, ktoon.encodeToString(value))
        assertEquals(value, ktoon.decodeFromString<Shape>(expected))
    }

    @Test
    fun `sealed values round-trip as list elements`() {
        val value = listOf<Shape>(Shape.Circle(1), Shape.Square(2))
        val encoded = ktoon.encodeToString(value)
        assertEquals(value, ktoon.decodeFromString<List<Shape>>(encoded))
    }

    @Test
    fun `sealed values round-trip as map values`() {
        val value = mapOf("a" to Shape.Circle(1) as Shape, "b" to Shape.Square(2))
        val encoded = ktoon.encodeToString(value)
        assertEquals(value, ktoon.decodeFromString<Map<String, Shape>>(encoded))
    }

    @Serializable data class OpenHolder(val note: @Serializable Note)

    private val openKtoon =
        Ktoon(SerializersModule { polymorphic(Note::class) { subclass(Memo::class) } }) {}

    @Test
    fun `an open polymorphic value keeps its own object`() {
        val value = OpenHolder(Memo("hi"))
        val expected = "note:\n  type: memo\n  value:\n    text: hi"
        assertEquals(expected, openKtoon.encodeToString(value))
        assertEquals(value, openKtoon.decodeFromString<OpenHolder>(expected))
    }
}
