# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ktoon** is a Kotlin implementation of the TOON (Token-Oriented Object Notation) format specification. TOON is a line-oriented, indentation-based text format that encodes the JSON data model with explicit structure and minimal quoting, particularly efficient for arrays of uniform objects.

This library integrates with Kotlin's `kotlinx.serialization` framework to provide encoding and decoding of TOON format.

## TOON Format Specification

**IMPORTANT**: `SPEC.md` is the authoritative specification document for the TOON format (v3.0). This implementation must conform to all normative requirements in the spec.

When working on this codebase:
- Always reference `@SPEC.md` for normative behavior and rules
- Code comments often include spec section references (e.g., "§7.2" for quoting rules)
- Test fixtures include `specSection` fields pointing to relevant spec sections
- The spec is organized into normative sections (§1-16, §19) and informative appendices

Key spec sections frequently referenced in code:
- **§2**: Data model and canonical number formatting
- **§6**: Array header syntax
- **§7**: String quoting and key encoding rules
- **§9**: Array format variations (inline, tabular, list)
- **§10**: Objects as list items
- **§11**: Delimiter scoping rules
- **§12**: Indentation and whitespace rules
- **§13**: Conformance requirements and optional features
- **§14**: Strict mode validation errors (authoritative checklist)

## Build Commands

This is a Gradle-based Kotlin project. Key commands:

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run all checks (includes tests and validation)
./gradlew check

# Clean build artifacts
./gradlew clean

# Run a single test class (example)
./gradlew test --tests "com.lukelast.ktoon.KtoonRoundTripTest"

# Run a single test method (example)
./gradlew test --tests "com.lukelast.ktoon.KtoonRoundTripTest.testBasicEncoding"

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Build JAR
./gradlew jar
```

## Architecture

### Core Components

The library is structured around three main processing stages:

1. **Encoding** (`lib/src/main/kotlin/com/lukelast/ktoon/encoding/`)
   - `ToonEncoder` - Root encoder implementing kotlinx.serialization's AbstractEncoder
   - `ToonWriter` - Low-level output writer managing indentation and formatting
   - `ToonObjectEncoder` - Handles object serialization with key folding support
   - `ToonArrayEncoder` - Handles array serialization with tabular/inline/list format detection
   - `ArrayFormatSelector` - Determines optimal array representation (tabular vs inline vs list)
   - `StringQuoting` - Implements TOON spec quoting rules (§7.2)
   - `NumberNormalizer` - Normalizes numeric values per spec (§2)

2. **Decoding** (`lib/src/main/kotlin/com/lukelast/ktoon/decoding/`)
   - `ToonDecoder` - Root decoder implementing kotlinx.serialization's AbstractDecoder
   - `ToonLexer` - Tokenizes TOON input into structured tokens
   - `ToonReader` - Parses tokens into ToonValue intermediate representation

3. **Configuration** (`ToonConfiguration.kt`)
   - Controls strict mode validation (default: enabled)
   - Key folding for compact nested object notation (default: disabled)
   - Path expansion for decoding dotted keys (default: disabled)
   - Delimiter selection: COMMA (default), TAB, or PIPE
   - Indentation size (default: 2 spaces)

### Key Design Patterns

- **Three-phase parsing**: Lexing → Parsing → Decoding
  - Lexer converts raw text to tokens
  - Reader parses tokens into intermediate ToonValue tree
  - Decoder maps ToonValue to target types using kotlinx.serialization descriptors

- **Format detection for arrays**: The `ArrayFormatSelector` automatically chooses between:
  - **Tabular format**: For uniform arrays of objects with primitive values only
  - **Inline format**: For primitive arrays
  - **List format**: For mixed/nested arrays

- **Strict vs Lenient modes**:
  - Strict mode (default) enforces array counts, indentation multiples, delimiter consistency
  - Lenient mode allows more flexible parsing (see `ToonConfiguration.Lenient`)

### Kotlinx.serialization Integration

The library uses kotlinx.serialization's encoder/decoder framework:
- Implements `AbstractEncoder` and `AbstractDecoder`
- Respects `@Serializable` annotations
- Supports custom serializers via `SerializersModule`
- Handles polymorphic and contextual serialization

## Testing

### Test Structure

- **Fixture-based tests** (`lib/src/test/resources/fixtures/`):
  - JSON files define test cases with input, expected output, and options
  - `FixtureTestUtils.kt` provides `runFixtureTest()` helper
  - Organized by feature: primitives, arrays, objects, delimiters, etc.

- **Round-trip tests** (`KtoonRoundTripTest.kt`):
  - Verify encode → decode → encode produces identical output

- **Property-based tests** (`InstancioRandomTest.kt`):
  - Uses Instancio to generate random test data

- **Spec conformance tests** (`lib/src/test/kotlin/com/lukelast/ktoon/data1/`):
  - Reference `.toon` files with expected behavior

### Running Specific Tests

```bash
# Run all fixture tests
./gradlew test --tests "*FixtureTest"

# Run encoding tests only
./gradlew test --tests "*EncodeTest"

# Run a specific test category
./gradlew test --tests "com.lukelast.ktoon.fixtures.test.ArraysTabularEncodeTest"
```

## Common Development Patterns

### Adding a New Test Case

1. Add test case to appropriate fixture JSON file in `lib/src/test/resources/fixtures/`
2. Create or update test class in `lib/src/test/kotlin/com/lukelast/ktoon/fixtures/test/`
3. Use `runFixtureTest("fixture-name", "test-name")` pattern

### Implementing Spec Changes

When implementing changes based on `@SPEC.md` updates:

1. Read the relevant spec section in `@SPEC.md` (e.g., §7.2 for quoting rules)
2. Update the corresponding component:
   - Encoding rules → `encoding/` package
   - Decoding rules → `decoding/` package
   - Validation → `validation/ValidationEngine.kt`
3. Add fixture tests referencing the spec section in `specSection` field
4. Update strict mode validation if applicable (see §14 checklist in `@SPEC.md`)
5. Add code comments citing the spec section for traceability

### Debugging Encoding Issues

The encoding process follows this flow:
1. `Ktoon.encodeToString()` creates a `ToonEncoder`
2. kotlinx.serialization calls encoder methods (encodeInt, encodeString, beginStructure, etc.)
3. `ToonWriter` accumulates output with indentation tracking
4. For arrays, `ArrayFormatSelector.selectFormat()` determines representation
5. For strings, `StringQuoting.needsQuoting()` checks if quotes required

Set breakpoints in:
- `ToonWriter.writeKey()` for key encoding issues
- `ArrayFormatSelector.selectFormat()` for array format selection
- `StringQuoting.needsQuoting()` for quoting logic

### Debugging Decoding Issues

The decoding process follows this flow:
1. `Ktoon.decodeFromString()` creates a `ToonLexer`
2. Lexer tokenizes input into `Token` objects
3. `ToonReader` parses tokens into `ToonValue` tree
4. `ToonDecoder` maps `ToonValue` to target types

Set breakpoints in:
- `ToonLexer.tokenize()` for tokenization issues
- `ToonReader.readRoot()` for parsing structure
- `ToonDecoder.decodeSerializableValue()` for type mapping

## Version Catalog

Dependencies are managed in `gradle/libs.versions.toml`:
- Kotlin 2.2.21
- kotlinx-serialization-json 1.9.0
- JUnit 5 for testing
- jtoon (reference TypeScript implementation) for cross-validation

## Publishing

The library is configured for JitPack publishing:
- Group: `com.github.lukelast`
- Artifact: `ktoon`
- See `jitpack.yml` for JitPack configuration
