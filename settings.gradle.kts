rootProject.name = "ktoon"

include(":ktoon", ":demo", ":benchmark")

val isJitPack = System.getenv("JITPACK") == "true"

if (!isJitPack) {
    includeBuild(":demo-kmp")
}
