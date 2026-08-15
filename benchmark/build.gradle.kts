plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
}

ktfmt { kotlinLangStyle() }

benchmark { targets { register("main") } }

dependencies {
    implementation(project(":ktoon"))
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlin.serialization)
    implementation(libs.instancio.core)

    // For performance comparison.
    implementation(libs.jtoon)
    implementation(libs.kotlinToon)
}
