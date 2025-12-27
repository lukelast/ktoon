plugins {
    alias(libs.plugins.kotlin.jvm.base)
    alias(libs.plugins.kotlin.serialization.base)
    `java-library`
    `maven-publish`
}

group = "com.github.lukelast"

version = "1-SNAPSHOT"

java { withSourcesJar() }

dependencies {
    api(libs.kotlin.serialization)

    // Test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform)
    testRuntimeOnly(libs.slf4j.simple)
    testImplementation(libs.instancio.junit)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "ktoon"
        }
    }
}
