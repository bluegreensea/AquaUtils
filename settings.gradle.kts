pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        gradlePluginPortal()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    }
}

rootProject.name = "AquaUtils"

include("common", "fabric", "velocity")
