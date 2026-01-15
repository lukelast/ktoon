pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "ktoon"

include(":ktoon", ":demo", ":demo-kmp", ":benchmark")
