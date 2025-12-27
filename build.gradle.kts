plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
