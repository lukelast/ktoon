# ktoon
[![Maven Central](https://img.shields.io/maven-central/v/com.lukelast.ktoon/ktoon)](https://central.sonatype.com/artifact/com.lukelast.ktoon/ktoon)
[![.github/workflows/gradle.yml](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml/badge.svg)](https://github.com/lukelast/ktoon/actions/workflows/gradle.yml)
[![SPEC v4.1.1](https://img.shields.io/badge/ToonSpec-v4.1.1-fef3c0?labelColor=1b1b1f)](https://github.com/toon-format/spec/blob/v4.1.1/SPEC.md)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
![Kotlin](https://img.shields.io/badge/Java-17+-yellow?logo=java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Kotlin serializer for TOON (Token-Oriented Object Notation).

To learn about the TOON format and why you should use it read the official website:
- https://toonformat.dev/
- TOON specification: https://github.com/toon-format/spec.


## Features

- **Full TOON 4.1.1 Spec Support** - Complete implementation of the TOON format specification, including tabular arrays with nested field groups, the keyed tabular form, comment-line stripping, and delimiters. Validated against the official spec fixture suite; 1,100+ tests.
- **Kotlin Multiplatform** - Supports JVM, Android, JavaScript, WebAssembly, and native targets (iOS, macOS, Linux, Windows).
- **Maven Central** - Published to Maven Central for easy dependency management with Gradle and Maven.
- **Fully Featured**
    - Encode Kotlin data classes to TOON
    - Decode TOON to Kotlin data classes
    - Convert JSON to TOON and TOON to JSON
- **Minimal Dependencies** - Only depends on kotlinx.serialization, no additional runtime dependencies.
- **High Performance** - CharArray-based encoding optimized for minimal allocations and fast string operations.
- **Flexible Configuration** - Configurable delimiter, indent size, strict mode, field sorting, and default encoding.

## Add to your project (Maven Central)

Using the Gradle Kotlin DSL:
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.lukelast.ktoon:ktoon:VERSION")
}
```

Using Gradle Version Catalog:
```toml
# gradle/libs.versions.toml
[versions]
ktoon = "VERSION"

[libraries]
ktoon = { module = "com.lukelast.ktoon:ktoon", version.ref = "ktoon" }
```
```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.ktoon)
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

Using Maven:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.lukelast.ktoon</groupId>
    <artifactId>ktoon-jvm</artifactId>
    <version>VERSION</version>
</dependency>
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
    // id: 1
    // name: Alice

    val decoded: User = Ktoon.Default.decodeFromString(encoded)
}
```

## Spec conformance notes

ktoon targets `toon-spec: 4.1`. Known deviations from the spec:

- **`sortFields = true`** (opt-in, off by default): emits object fields in sorted order rather
  than the encounter order the spec requires.

Documented limits (SPEC §15): values and documents nested deeper than `maxNestingDepth`
containers (default 128) are rejected with a normal error on both encode and decode instead of
exhausting the host stack. The same configurable limit applies to both sides, so anything the
library encodes it can also decode.

## Dependencies

* This library is built to target Java 17.
* You need kotlinx serialization which requires a build plugin.
  * https://github.com/Kotlin/kotlinx.serialization

## Demo project

Check out the demo project in the `demo` directory for more examples on how to use `Ktoon`.
[demo/README.md](demo/README.md)

## Development
See the [development guide](DEV.md) for how to do development.
