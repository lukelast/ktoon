@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

kotlin {
    jvm { mainRun { mainClass.set("com.lukelast.ktoon.demo.MainKt") } }

    js {
        nodejs { binaries.executable() }
        browser()
    }

    sourceSets { commonMain.dependencies { implementation(project(":ktoon")) } }
}
