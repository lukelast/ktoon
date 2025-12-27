plugins {
    alias(libs.plugins.kotlin.jvm.base)
    alias(libs.plugins.kotlin.serialization.base)
    alias(libs.plugins.kotlinx.benchmark.base)
}

benchmark { targets { register("main") } }

dependencies {
    implementation(project(":lib"))
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlin.serialization)
    implementation(libs.instancio.junit)

    // For performance comparison.
    implementation(libs.jtoon)
    implementation(libs.kotlinToon)
}
