# Development Guide

Use the Gradle wrapper (`./gradlew`) and JDK 17+.

## Testing
- `./gradlew test` — run all tests.
- `./gradlew clean`

## Benchmarks
- `./gradlew :benchmark:benchmark` — run all kotlinx-benchmark targets.
- Reports: HTML/JSON under `benchmark/build/reports/benchmarks/`.

## Demo app
- `./gradlew :demo:run` — quick end-to-end check that encoding/decoding works in a runnable app.
