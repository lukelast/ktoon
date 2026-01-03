plugins {
    alias(libs.plugins.kotlin.multiplatform.base)
    alias(libs.plugins.kotlin.serialization.base)
    alias(libs.plugins.maven.publish)
}

group = "com.lukelast.ktoon"

version = providers.gradleProperty("version").orElse("0.0.0-SNAPSHOT").get()

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

    js {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.library()
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
