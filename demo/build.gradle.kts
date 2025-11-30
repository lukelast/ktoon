plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Normally add the dependency from JitPack like this
    // implementation("com.github.lukelast:ktoon:VERSION")

    implementation(project(":lib"))

    implementation(libs.kotlin.serialization)
}

application { mainClass.set("com.lukelast.ktoon.demo.encode.DefaultKt") }

tasks.test { useJUnitPlatform() }
