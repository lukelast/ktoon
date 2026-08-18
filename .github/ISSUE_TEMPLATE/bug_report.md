---
name: Bug report
about: Report incorrect encoding/decoding, a crash, or a spec deviation
title: ''
labels: bug
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**Minimal reproducer**
The Kotlin code, and the input data, needed to reproduce the problem.

```kotlin
@Serializable
data class Example(val id: Int, val name: String)

val ktoon = Ktoon.Default
println(ktoon.encodeToString(Example(1, "Alice")))
```

**Expected behavior**
The TOON output (or decoded value) you expected.

```toon

```

**Actual behavior**
The TOON output (or decoded value) you actually got. If an exception was thrown, paste the full
stack trace here.

```

```

**Ktoon configuration**
Anything non-default you passed to `Ktoon { ... }` / `KtoonConfiguration`, e.g. `delimiter`,
`indentSize`, `strictMode`, `sortFields`, `encodeDefaults`. Say "defaults" if you used
`Ktoon.Default`.

**Spec reference (if applicable)**
If you believe the output violates the [TOON specification](https://github.com/toon-format/spec),
link the relevant section or fixture.

**Environment**
 - ktoon version: [e.g. 1.2.0]
 - Kotlin version: [e.g. 2.3.20]
 - kotlinx.serialization version: [e.g. 1.9.0]
 - Target/platform: [e.g. JVM, JS, WasmJs, Android, iosArm64, linuxX64, mingwX64]
 - JDK / runtime version (if relevant): [e.g. Temurin 21]
 - Build tool: [e.g. Gradle 9.x, Maven]

**Additional context**
Add any other context about the problem here.
