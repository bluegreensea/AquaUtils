plugins {
    kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-velocity") version "2.1.0"
}
val velocityVersion: String by project
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    maven("https://maven.elytrium.net/repo/") {
        name = "elytrium-repo"
    }
}
dependencies {
    implementation(rootProject.project("common"))

    // compileOnly("com.google.code.gson:gson:2.10.1")
    // val adventureVersion: String by project
    // compileOnly("net.kyori:adventure-api:$adventureVersion")
    // compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    kapt("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")
    val nettyVersion: String by project
    compileOnly("io.netty:netty-transport:$nettyVersion")

    val cloudVersion: String by project
    implementation("cloud.commandframework:cloud-velocity:$cloudVersion")
    compileOnly("com.electronwill.night-config:toml:3.6.6")

    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar")))) // VelocityGUI
    compileOnly("net.elytrium.limboapi:api:1.1.16")
    compileOnly("dev.simplix:protocolize-velocity:2.2.6")
}
tasks {
    register<Copy>("distRun") {
        dependsOn(shadowJar)
        group = "shadow"
        from(shadowJar.get().archiveFile.get())
        into("run/plugins")
    }
    runVelocity {
        velocityVersion(velocityVersion)
    }
    shadowJar {
        archiveClassifier.set("with-dependencies")
        archiveVersion.set("")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
