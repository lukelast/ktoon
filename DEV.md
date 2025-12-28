# Development Guide

Use the Gradle wrapper (`./gradlew`) and JDK 17+.

## Project Structure

This is a Kotlin Multiplatform project with JVM and JavaScript (browser) targets.

```
ktoon/src/
  commonMain/kotlin/    # Main library code (platform-agnostic)
  commonTest/kotlin/    # Cross-platform tests
  jvmTest/kotlin/       # JVM-only tests (fixtures, data1, rand)
```

## Building

- `./gradlew :ktoon:build` — build all targets
- `./gradlew :ktoon:compileKotlinJvm` — compile JVM target
- `./gradlew :ktoon:compileKotlinJs` — compile JS target
- `./gradlew kotlinUpgradeYarnLock` — update kotlin-js-store/yarn.lock

## Testing

- `./gradlew clean`
- `./gradlew :ktoon:allTests` — run tests on all targets
- `./gradlew :ktoon:jvmTest` — run JVM tests (all 425 tests)
- `./gradlew :ktoon:jsBrowserTest` — run JS browser tests (commonTest only)

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

## Maintenance


