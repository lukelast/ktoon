package com.lukelast.ktoon.fixtures

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlinx.serialization.json.JsonPrimitive

class ParseTest {
    @Test
    fun `loads primitives fixture`() {
        val fixture = loadFixture("fixtures/encode/primitives.json")

        assertEquals("1.4", fixture.version)
        assertEquals(FixtureCategory.encode, fixture.category)
        assertEquals("Primitive value encoding - strings, numbers, booleans, null", fixture.description)
        assertTrue(fixture.tests.isNotEmpty())

        println("Loaded ${fixture.tests.size} test cases from primitives.json")
    }

    @Test
    fun `loads all encode fixtures`() {
        val fixtures = loadEncodeFixtures()

        assertTrue(fixtures.isNotEmpty())
        assertTrue("primitives.json" in fixtures.keys)
        assertTrue("arrays-primitive.json" in fixtures.keys)

        println("Loaded ${fixtures.size} encode fixture files")
        fixtures.forEach { (filename, fixture) ->
            println("  - $filename: ${fixture.description} (${fixture.tests.size} tests)")
        }
    }

    @Test
    fun `converts options to ToonConfiguration`() {
        val options = FixtureOptions(
            delimiter = "\t",
            indent = 4,
            strict = false,
            keyFolding = "safe",
            expandPaths = "safe"
        )

        val config = options.toToonConfiguration()

        assertEquals(com.lukelast.ktoon.ToonConfiguration.Delimiter.TAB, config.delimiter)
        assertEquals(4, config.indentSize)
        assertEquals(false, config.strictMode)
        assertEquals(true, config.keyFolding)
        assertEquals(true, config.pathExpansion)
    }

    @Test
    fun `extracts string from JsonElement`() {
        val fixture = loadFixture("fixtures/encode/primitives.json")
        val firstTest = fixture.tests.first()

        // The expected value should be a string for encode tests
        val expectedString = firstTest.expected.asString()
        assertNotNull(expectedString)

        println("First test: ${firstTest.name}")
        println("  Expected: $expectedString")
    }

    @Test
    fun `checks fixture category`() {
        val fixture = loadFixture("fixtures/encode/primitives.json")

        assertTrue(fixture.isEncode())
        assertFalse(fixture.isDecode())
    }

    @Test
    fun `parses test case with all optional fields`() {
        val fixture = loadFixture("fixtures/encode/primitives.json")

        // Find a test with specSection
        val testWithSpec = fixture.tests.find { it.specSection != null }
        assertNotNull(testWithSpec)

        println("Test with spec section: ${testWithSpec?.name}")
        println("  Spec section: ${testWithSpec?.specSection}")
        println("  Note: ${testWithSpec?.note ?: "(none)"}")
    }
}
