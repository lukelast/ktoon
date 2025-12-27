plugins {
    alias(libs.plugins.kotlin.jvm.base)
    alias(libs.plugins.kotlin.serialization.base)
    alias(libs.plugins.kotlinx.benchmark.base)
    `java-library`
    `maven-publish`
}

group = "com.github.lukelast"

version = "1-SNAPSHOT"

repositories { mavenCentral() }

java { withSourcesJar() }

sourceSets {
    val main: SourceSet by getting
    val benchmark: SourceSet by creating {
        compileClasspath += main.output
        runtimeClasspath += main.output
    }
}

kotlin {
    target { compilations.getByName("benchmark").associateWith(compilations.getByName("main")) }
}

benchmark { targets { register("benchmark") } }

dependencies {
    api(libs.kotlin.serialization)

    // Test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.platform)
    testRuntimeOnly(libs.slf4j.simple)
    testImplementation(libs.instancio.junit)
    // Comparison libraries
    testImplementation(libs.jtoon)
    testImplementation(libs.kotlinToon)

    // Benchmark dependencies
    "benchmarkImplementation"(libs.kotlinx.benchmark.runtime)
    "benchmarkImplementation"(libs.kotlin.serialization)
    "benchmarkImplementation"(libs.instancio.junit)
    // Comparison libraries
    "benchmarkImplementation"(libs.jtoon)
    "benchmarkImplementation"(libs.kotlinToon)
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
