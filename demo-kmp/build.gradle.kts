@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
}

ktfmt { kotlinLangStyle() }

// See the comment in ktoon/build.gradle.kts: the plain `detekt` task has no sources in a KMP
// layout. detektMainJvm covers commonMain + jvmMain with type resolution; jsMain is not part of
// any JVM compilation, so it needs its own source set task.
tasks.named("detekt") {
    dependsOn(tasks.named("detektMainJvm"), tasks.named("detektJsMainSourceSet"))
}

kotlin {
    jvm { mainRun { mainClass.set("com.lukelast.ktoon.demo.MainKt") } }

    js {
        nodejs { binaries.executable() }
        browser()
    }

    sourceSets { commonMain.dependencies { implementation(project(":ktoon")) } }
}
