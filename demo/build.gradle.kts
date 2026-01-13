plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Normally add the dependency from maven central like:
    // implementation("com.lukelast.ktoon:ktoon:VERSION")

    implementation(project(":ktoon"))
}

application { mainClass.set("com.lukelast.ktoon.demo.encode.DefaultKt") }

tasks.test { useJUnitPlatform() }
