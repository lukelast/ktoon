# Development Guide

Use the Gradle wrapper (`./gradlew`) and JDK 17+.

## Project Structure

This is a Kotlin Multiplatform project with JVM and JavaScript (browser) targets.

```
lib/src/
  commonMain/kotlin/    # Main library code (platform-agnostic)
  commonTest/kotlin/    # Cross-platform tests
  jvmTest/kotlin/       # JVM-only tests (fixtures, data1, rand)
```

## Building

- `./gradlew :lib:compileKotlinJvm` — compile JVM target
- `./gradlew :lib:compileKotlinJs` — compile JS target
- `./gradlew :lib:build` — build all targets

## Testing

- `./gradlew clean`
- `./gradlew :lib:allTests` — run tests on all targets
- `./gradlew :lib:jvmTest` — run JVM tests (all 425 tests)
- `./gradlew :lib:jsBrowserTest` — run JS browser tests (commonTest only)

## Benchmarks

- `./gradlew :benchmark:benchmark` — run all kotlinx-benchmark targets
- Reports: HTML/JSON under `benchmark/build/reports/benchmarks/`

## Demo app

- `./gradlew :demo:run` — quick end-to-end check that encoding/decoding works

## Publishing

- `./gradlew :lib:publishToMavenLocal` — publish to local Maven repository

Published artifacts:
- `ktoon` — Gradle metadata module
- `ktoon-jvm` — JVM artifact
- `ktoon-js` — JavaScript artifact
