# ktoon
[![](https://jitpack.io/v/lukelast/ktoon.svg)](https://jitpack.io/#lukelast/ktoon)
[![.github/workflows/gradle.yml](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml/badge.svg)](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml)

Kotlin serializer for TOON (Token-Oriented Object Notation).

To learn about the TOON format and why you should use it read the official website:
- https://toonformat.dev/
- TOON specification: https://github.com/toon-format/spec.


## Features

- **Full TOON 3.0 Spec Support** - Complete implementation of the TOON format specification, including tabular arrays, key folding, and delimeters.
- **Fully Featured**
    - Encode Kotlin data classes to TOON
    - Encode JSON to TOON
    - Decode TOON to Kotlin data classes
- **Minimal Dependencies** - Only depends on kotlinx.serialization, no additional runtime dependencies.
- **High Performance** - CharArray-based encoding optimized for minimal allocations and fast string operations, inspired by kotlinx.serialization internals.
- **Flexible Configuration** - Configurable delimiters, indentation, and key folding.

## Add to your project (JitPack)

Check JitPack for versions and more installation instructions:
https://jitpack.io/#lukelast/ktoon

Using the Gradle Kotlin DSL:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.lukelast:ktoon:VERSION")
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
