plugins {
    id("fabric-loom")
}
repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}
dependencies {
    include(implementation(rootProject.project("common"))!!)

    val minecraftVersion: String by project
    minecraft("com.mojang", "minecraft", minecraftVersion)
    // val yarnMappings: String by project
    // mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
    mappings(loom.officialMojangMappings())
    val loaderVersion: String by project
    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)

    val fabricVersion: String by project
    modRuntimeOnly("net.fabricmc.fabric-api", "fabric-api", fabricVersion)
    modImplementation(fabricApi.module("fabric-command-api-v2", fabricVersion))
    modImplementation(fabricApi.module("fabric-networking-api-v1", fabricVersion))
    modImplementation(fabricApi.module("fabric-rendering-v1", fabricVersion))
    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc", "fabric-language-kotlin", fabricKotlinVersion)

    val adventurePlatformFabricVersion: String by project
    include(modImplementation("net.kyori:adventure-platform-fabric:$adventurePlatformFabricVersion")!!)
    val cloudVersion: String by project
    include(modImplementation("org.incendo:cloud-fabric:$cloudVersion")!!)
}
tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
}
