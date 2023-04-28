plugins {
    id("fabric-loom")
}
dependencies {
    api(rootProject.project("common"))
    val minecraftVersion: String by project
    minecraft("com.mojang", "minecraft", minecraftVersion)
    // val yarnMappings: String by project
    // mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
    mappings(loom.officialMojangMappings())
    val loaderVersion: String by project
    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)
    val fabricVersion: String by project
    // modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)
    modImplementation(fabricApi.module("fabric-command-api-v2", fabricVersion))
    modImplementation(fabricApi.module("fabric-networking-api-v1", fabricVersion))
    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc", "fabric-language-kotlin", fabricKotlinVersion)
    // modImplementation(include("net.kyori:adventure-platform-fabric:5.6.0")!!)
}
tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
}
