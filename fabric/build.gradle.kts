plugins {
    id("fabric-loom")
}
dependencies {
    include(implementation(project(":common"))!!)

    val minecraftVersion: String by project
    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings(loom.officialMojangMappings())
    val fabricLoaderVersion: String by project
    modImplementation("net.fabricmc", "fabric-loader", fabricLoaderVersion)

    val fabricVersion: String by project
    modRuntimeOnly("net.fabricmc.fabric-api", "fabric-api", fabricVersion)
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabricVersion))
    modImplementation(fabricApi.module("fabric-message-api-v1", fabricVersion))
    modImplementation(fabricApi.module("fabric-networking-api-v1", fabricVersion))
    modImplementation(fabricApi.module("fabric-rendering-v1", fabricVersion))
    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc", "fabric-language-kotlin", fabricKotlinVersion)

    val adventurePlatformFabricVersion: String by project
    include(modImplementation("net.kyori:adventure-platform-mod-shared-fabric-repack:$adventurePlatformFabricVersion")!!)
    val cloudMinecraftModdedVersion: String by project
    include(modImplementation("org.incendo:cloud-fabric:$cloudMinecraftModdedVersion")!!)
}
tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mapOf("version" to project.version)) }
    }
}
