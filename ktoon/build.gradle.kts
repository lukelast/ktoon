@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

group = "com.lukelast.ktoon"

version = providers.gradleProperty("version").orElse("0.0.0-SNAPSHOT").get()

kotlin {
    applyDefaultHierarchyTemplate()
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
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

    js {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.library()
    }

    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    wasmWasi {
        nodejs()
        binaries.library()
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // native target "tiers" here are taken from https://kotlinlang.org/docs/native-target-support.html
    // tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    // tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()

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

android {
    namespace = group.toString()
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("ktoon")
        description.set("TOON format serialization for Kotlin Multiplatform")
        url.set("https://github.com/lukelast/ktoon")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        scm {
            url.set("https://github.com/lukelast/ktoon")
            connection.set("scm:git:https://github.com/lukelast/ktoon.git")
            developerConnection.set("scm:git:ssh://git@github.com/lukelast/ktoon.git")
        }

        developers {
            developer {
                id.set("lukelast")
                name.set("Luke Last")
                url.set("https://github.com/lukelast")
            }
        }
    }
}
