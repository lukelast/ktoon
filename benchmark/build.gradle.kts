plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.benchmark)
}

benchmark { targets { register("main") } }

dependencies {
    implementation(project(":ktoon"))
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlin.serialization)
    implementation(libs.instancio.junit)

    // For performance comparison.
    implementation(libs.jtoon)
    implementation(libs.kotlinToon)
}
