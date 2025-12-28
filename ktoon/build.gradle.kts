plugins {
    alias(libs.plugins.kotlin.multiplatform.base)
    alias(libs.plugins.kotlin.serialization.base)
    `maven-publish`
}

group = "com.github.lukelast"

version = "1-SNAPSHOT"

val isJitPack = System.getenv("JITPACK") == "true"

kotlin {
    applyDefaultHierarchyTemplate()
    jvm {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
        testRuns["test"].executionTask.configure {
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
        withSourcesJar()
    }

    if (!isJitPack) {
        js {
            browser { testTask { useKarma { useChromeHeadless() } } }
            binaries.library()
        }
    }

    sourceSets {
        commonMain.dependencies { api(libs.kotlin.serialization) }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform)
            runtimeOnly(libs.slf4j.simple)
            implementation(libs.instancio.junit)
            implementation(kotlin("reflect"))
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            description.set("TOON format serialization for Kotlin Multiplatform")
        }
    }
}
