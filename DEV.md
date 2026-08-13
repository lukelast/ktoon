# Development Guide

Use the Gradle wrapper (`./gradlew`) and JDK 17+.

## Project Structure

This is a Kotlin Multiplatform project.

```
ktoon/src/
  commonMain/kotlin/    # Main library code (platform-agnostic)
  commonTest/kotlin/    # Cross-platform tests
  jvmTest/kotlin/       # JVM-only tests that use json test files
```

## Building

- `./gradlew :ktoon:build` — build all targets
- `./gradlew :ktoon:compileKotlinJvm` — compile JVM target
- `./gradlew :ktoon:compileKotlinJs` — compile JS target
- `./gradlew kotlinUpgradeYarnLock` — update kotlin-js-store/yarn.lock
- `./gradlew ktfmtFormat` — format all Kotlin sources
- `./gradlew detekt` — static analysis

## Testing

- `./gradlew clean check`
- `./gradlew :ktoon:allTests` — run tests on all targets
- `./gradlew :ktoon:jvmTest` — run JVM tests
- `./gradlew :ktoon:jsBrowserTest` — run JS browser tests

## Benchmarks

- `./gradlew :benchmark:benchmark` — run all kotlinx-benchmark targets
- Reports: HTML/JSON under `benchmark/build/reports/benchmarks/`

## Demo apps

- `./gradlew :demo:run` — JVM-only demo
- `./gradlew :demo-kmp:jvmRun` — KMP demo (JVM)
- `./gradlew :demo-kmp:jsNodeDevelopmentRun` — KMP demo (Node.js)

## Publishing

- `./gradlew :ktoon:publishToMavenLocal` — publish to local Maven repository

Published artifacts:
- `ktoon` — Gradle metadata module
- `ktoon-jvm` — JVM artifact
- `ktoon-js` — JavaScript artifact
