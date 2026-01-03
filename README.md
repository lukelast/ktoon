# ktoon
[![Maven Central](https://img.shields.io/maven-central/v/com.lukelast.ktoon/ktoon)](https://central.sonatype.com/artifact/com.lukelast.ktoon/ktoon)
[![.github/workflows/gradle.yml](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml/badge.svg)](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml)
[![SPEC v3.0.1](https://img.shields.io/badge/ToonSpec-v3.0.1-fef3c0?labelColor=1b1b1f)](https://github.com/toon-format/spec/blob/v3.0.1/SPEC.md)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
![Kotlin](https://img.shields.io/badge/Java-17+-yellow?logo=java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Kotlin serializer for TOON (Token-Oriented Object Notation).

To learn about the TOON format and why you should use it read the official website:
- https://toonformat.dev/
- TOON specification: https://github.com/toon-format/spec.


## Features

- **Full TOON 3.0.1 Spec Support** - Complete implementation of the TOON format specification, including tabular arrays, key folding, and delimeters. 400+ tests.
- **Fully Featured**
    - Encode Kotlin data classes to TOON
    - Encode JSON to TOON
    - Decode TOON to Kotlin data classes
- **Minimal Dependencies** - Only depends on kotlinx.serialization, no additional runtime dependencies.
- **High Performance** - CharArray-based encoding optimized for minimal allocations and fast string operations.
- **Flexible Configuration** - Configurable delimiters, indentation, and key folding.

## Add to your project (Maven Central)

Using the Gradle Kotlin DSL:
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.lukelast.ktoon:ktoon:VERSION")
}
```

For multiplatform projects:
```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.lukelast.ktoon:ktoon:VERSION")
        }
    }
}
```

## Basic usage
```kotlin
import com.lukelast.ktoon.Ktoon
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String)

fun main() {
    val encoded = Ktoon.Default.encodeToString(User(1, "Alice"))
    println(encoded)
}
```

## Dependencies

* This library is built to target Java 17.
* You need kotlinx serialization which requires a build plugin.
  * https://github.com/Kotlin/kotlinx.serialization

## Demo project

Check out the demo project in the `demo` directory for more examples on how to use `Ktoon`.
[demo/README.md](demo/README.md))

## Development
See the [development guide](DEV.md) for how to do development.
