plugins {
    id("net.kyori.blossom")
}
repositories {
    maven("https://libraries.minecraft.net")
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    compileOnly("com.mojang:brigadier:1.0.18")

    compileOnly("com.google.code.gson:gson:2.13.0")

    val adventureVersion: String by project
    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.0")

    implementation("org.jsoup:jsoup:1.19.1")
    implementation("com.electronwill.night-config:toml:3.6.7")

    val nettyVersion: String by project
    compileOnly("io.netty:netty-buffer:$nettyVersion")
}
sourceSets {
    main {
        blossom {
            kotlinSources {
                property("version", version.toString())
            }
        }
    }
}
