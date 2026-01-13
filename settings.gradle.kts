pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "ktoon"

include(":ktoon", ":demo", ":benchmark")

include(":demo-kmp")
