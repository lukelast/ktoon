# Repository Guidelines

## Project Structure & Module Organization
- Gradle multi-project with a single library module `lib`; source in `lib/src/main/kotlin/com/lukelast/ktoon/*`.
- Tests and fixtures live in `lib/src/test/kotlin`; JSON/TOON fixture pairs sit beside the tests (e.g., `data1/test01/data.{json,toon}`).
- Shared docs: `SPEC.md` (format spec) and `README.md` (top-level overview). Update these when behavior changes.
- Build scripts use Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`) and centralized versions in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
- `./gradlew build` — full compile + test; preferred pre-push gate.
- `./gradlew test` — run unit/fixture tests; uses JUnit Platform.
- `./gradlew publishToMavenLocal` — build and publish the library locally for downstream testing.
- Use the wrapper (`./gradlew …`) to keep the expected Gradle/Kotlin versions.

## Coding Style & Naming Conventions
- Kotlin, 4-space indents, trailing commas avoided; keep imports explicit and ordered by package.
- Packages live under `com.lukelast.ktoon`; new public APIs should stay in that namespace.
- Prefer expression-bodied functions only for simple returns; favor readable blocks for encoder/decoder logic.
- Serialization: use kotlinx.serialization annotations; keep configuration defaults in `ToonConfiguration` and factory helpers in `Ktoon`.
- Add brief KDoc for public types and non-obvious algorithms (e.g., lexer/encoder rules).

## Testing Guidelines
- Framework: JUnit (Jupiter) with standard assertions; random data via Instancio where present.
- Naming: mirror fixture folders (`data1/test04/Test04Test.kt`) and keep paired `data.json` / `data.toon` samples beside the test.
- Run `./gradlew test` before submitting; add new fixtures when introducing format edge cases (delimiters, folding, whitespace).
- Keep tests deterministic; avoid depending on system locale/timezone.

## Commit & Pull Request Guidelines
- Commit messages: short, present tense, imperative (e.g., `fix more tests`, `run gradle check`); avoid long bodies unless needed.
- Pull requests should include: scope summary, rationale, key commands run, and any spec references (`SPEC.md` section numbers) when altering format rules.
- Link issues when applicable and add screenshots or sample TOON snippets if the change affects encoded output or error messages.
- Ensure CI-equivalent command (`./gradlew build`) passes locally before requesting review.

## Security & Configuration Tips
- No secrets should enter the repo; sample data must be synthetic or sanitized.
- Respect strict-mode defaults; changes to validation rules should document trade-offs in `SPEC.md` and tests.
- Keep dependency updates aligned with `gradle/libs.versions.toml`; avoid version drift by using the shared catalog entries.
