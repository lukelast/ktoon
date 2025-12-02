package com.lukelast.ktoon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// Sealed class hierarchies for polymorphic tests
@Serializable
sealed class Animal {
    @Serializable
    @SerialName("dog")
    data class Dog(val breed: String, val goodBoy: Boolean) : Animal()

    @Serializable @SerialName("cat") data class Cat(val name: String, val lives: Int) : Animal()
}

@Serializable
sealed class Shape {
    @Serializable @SerialName("circle") data class Circle(val radius: Double) : Shape()

    @Serializable
    @SerialName("rectangle")
    data class Rectangle(val width: Double, val height: Double) : Shape()

    @Serializable
    @SerialName("triangle")
    data class Triangle(val base: Double, val height: Double) : Shape()
}

@Serializable sealed class Message

@Serializable @SerialName("text") data class TextMessage(val content: String) : Message()

@Serializable
@SerialName("image")
data class ImageMessage(val url: String, val width: Int, val height: Int) : Message()

@Serializable
sealed class Result {
    @Serializable @SerialName("success") data class Success(val value: String) : Result()

    @Serializable
    @SerialName("error")
    data class Error(val message: String, val code: Int) : Result()
}

@Serializable
sealed class Node {
    @Serializable @SerialName("leaf") data class Leaf(val value: Int) : Node()

    @Serializable @SerialName("branch") data class Branch(val left: Node, val right: Node) : Node()
}

@Serializable
sealed class Event {
    @Serializable @SerialName("click") data class Click(val x: Int, val y: Int) : Event()

    @Serializable @SerialName("keypress") data class KeyPress(val key: String) : Event()
}

@Serializable
sealed class Item {
    @Serializable
    @SerialName("product")
    data class Product(val name: String, val price: Double) : Item()

    @Serializable
    @SerialName("service")
    data class Service(val description: String, val hourlyRate: Double) : Item()
}

/** Tests for polymorphic serialization support. */
@Disabled
class KtoonPolymorphicTest {

    @Test
    fun `encode sealed class with type discriminator`() {
        @Serializable data class Pet(val owner: String, val animal: Animal)

        val ktoon = Ktoon()
        val pet = Pet("Alice", Animal.Dog("Golden Retriever", true))

        val encoded = ktoon.encodeToString(pet)

        // Should contain type discriminator
        assertTrue(encoded.contains("type"))
        assertTrue(encoded.contains("dog"))
        assertTrue(encoded.contains("breed"))
        assertTrue(encoded.contains("Golden Retriever"))
    }

    @Test
    fun `round trip sealed class hierarchy`() {
        @Serializable data class Drawing(val name: String, val shapes: List<Shape>)

        val ktoon = Ktoon()
        val original =
            Drawing(
                name = "My Drawing",
                shapes =
                    listOf(
                        Shape.Circle(5.0),
                        Shape.Rectangle(10.0, 20.0),
                        Shape.Triangle(8.0, 12.0),
                    ),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Drawing>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `polymorphic with custom serializers module`() {
        @Serializable data class Chat(val id: String, val messages: List<Message>)

        val module = SerializersModule {
            polymorphic(Message::class) {
                subclass(TextMessage::class)
                subclass(ImageMessage::class)
            }
        }

        val ktoon = Ktoon(serializersModule = module)
        val original =
            Chat(
                id = "chat-123",
                messages =
                    listOf(
                        TextMessage("Hello!"),
                        ImageMessage("https://example.com/pic.jpg", 800, 600),
                        TextMessage("How are you?"),
                    ),
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Chat>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `polymorphic nullable sealed class`() {
        @Serializable data class Response(val id: Int, val result: Result?)

        val ktoon = Ktoon()

        // Test with non-null result
        val successResponse = Response(1, Result.Success("OK"))
        val successEncoded = ktoon.encodeToString(successResponse)
        val successDecoded = ktoon.decodeFromString<Response>(successEncoded)
        assertEquals(successResponse, successDecoded)

        // Test with null result
        val nullResponse = Response(2, null)
        val nullEncoded = ktoon.encodeToString(nullResponse)
        val nullDecoded = ktoon.decodeFromString<Response>(nullEncoded)
        assertEquals(nullResponse, nullDecoded)

        // Test with error result
        val errorResponse = Response(3, Result.Error("Not found", 404))
        val errorEncoded = ktoon.encodeToString(errorResponse)
        val errorDecoded = ktoon.decodeFromString<Response>(errorEncoded)
        assertEquals(errorResponse, errorDecoded)
    }

    @Test
    fun `nested polymorphic types`() {
        @Serializable data class Tree(val root: Node)

        val ktoon = Ktoon()
        val original =
            Tree(
                root =
                    Node.Branch(
                        left = Node.Leaf(1),
                        right = Node.Branch(left = Node.Leaf(2), right = Node.Leaf(3)),
                    )
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Tree>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `polymorphic with default discriminator`() {
        @Serializable data class EventLog(val timestamp: Long, val event: Event)

        val ktoon = Ktoon()
        val original = EventLog(1234567890L, Event.Click(100, 200))

        val encoded = ktoon.encodeToString(original)

        // Verify type field is present
        assertTrue(encoded.contains("type:") || encoded.contains("\"type\":"))
        assertTrue(encoded.contains("click"))

        val decoded = ktoon.decodeFromString<EventLog>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `list of polymorphic types`() {
        @Serializable data class Invoice(val items: List<Item>, val total: Double)

        val ktoon = Ktoon()
        val original =
            Invoice(
                items =
                    listOf(
                        Item.Product("Laptop", 999.99),
                        Item.Service("Consulting", 150.0),
                        Item.Product("Mouse", 29.99),
                    ),
                total = 1179.98,
            )

        val encoded = ktoon.encodeToString(original)

        val decoded = ktoon.decodeFromString<Invoice>(encoded)
        assertEquals(original, decoded)
    }
}
