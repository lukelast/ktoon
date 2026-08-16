plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
}

ktfmt { kotlinLangStyle() }

benchmark {
    configurations {
        // Runs quickly to make sure the benchmarks are working.
        named("main") {
            warmups = 1
            iterations = 1
            iterationTime = 250
            iterationTimeUnit = "ms"
        }
        // The fair library comparison — the only configuration that runs the other libraries.
        // No time overrides: the (uniform) annotation settings apply to every library.
        register("compare") {
            include("ComparisonBenchmark")
            advanced("jvmForks", 3)
        }
        // Iterating on ktoon performance: ktoon-only, fast but meaningful runs.
        register("dev") {
            include("KtoonBenchmark")
            include("StringQuoting")
            warmups = 2
            iterations = 5
            iterationTime = 2
            iterationTimeUnit = "s"
        }
    }
    targets { register("main") }
}

// Silence output noise: slf4j-api arrives via Instancio with no provider, and JDK 24+ warns about
// JMH's sun.misc.Unsafe use. JMH forks inherit these.
tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dslf4j.internal.verbosity=ERROR")
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            // Unsafe option does not exist before 24.
            val jvmMajor = javaLauncher.map { it.metadata.languageVersion.asInt() }.get()
            if (jvmMajor >= 24) listOf("--sun-misc-unsafe-memory-access=allow") else listOf()
        }
    )
}

dependencies {
    implementation(project(":ktoon"))
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlin.serialization)
    implementation(libs.instancio.core)

    // For performance comparison.
    implementation(libs.jtoon)
    implementation(libs.kotlinToon)
}
