plugins {
    id("de.fayard.refreshVersions") version "0.51.0"
}

refreshVersions {
    enableBuildSrcLibs()
}

rootProject.name = "runetools"

include(":asm")
include(":mapper")
include(":deobfuscator")