plugins {
    id("net.kyori.blossom") version "2.1.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
}
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    compileOnly("com.google.code.gson:gson:2.10.1")

    val adventureVersion: String by project
    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("org.slf4j:slf4j-api:2.0.7")
    compileOnly("org.apache.logging.log4j:log4j-api:2.20.0")

    implementation("org.jsoup:jsoup:1.15.4")

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
