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

- `./gradlew :benchmark:benchmark` — quick smoke run of every benchmark (seconds; verifies wiring, not for measuring; used by CI)
- `./gradlew :benchmark:devBenchmark` — ktoon-only benchmarks with meaningful run times, for iterating on performance work
- `./gradlew :benchmark:compareBenchmark` — performance comparison against other libraries
- Reports: HTML/JSON under `benchmark/build/reports/benchmarks/`

### Profiling with JFR

Build the executable JMH jar, then run it directly with JMH's built-in JFR profiler
(records inside each forked measurement JVM, one `.jfr` per benchmark and fork):

```
./gradlew :benchmark:mainBenchmarkJar
java -jar benchmark/build/benchmarks/main/jars/benchmark-main-jmh-JMH.jar encodeTabular -f 1 -prof jfr
```

The trailing argument is a regex over benchmark names, so `encodeTabular` profiles one path and
`KtoonBenchmark` profiles all of them. `-l <regex>` lists what a regex would select without running it.

Useful flags: `-prof gc` (allocation rates), `-p inputType=long_string` (pin a param),
`-wi`/`-i`/`-r` (shorten runs while profiling), `-prof jfr:help` (output options).

## Demo apps

- `./gradlew :demo:run` — JVM-only demo
- `./gradlew :demo-kmp:jvmRun` — KMP demo (JVM)
- `./gradlew :demo-kmp:jsNodeDevelopmentRun` — KMP demo (Node.js)

## Publishing

- `./gradlew :ktoon:publishToMavenLocal` — publish to local Maven repository
