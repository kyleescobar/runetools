plugins {
    kotlin("jvm") version "1.7.10"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "dev.runetools"
    version = "1.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "11"
        }
    }
}