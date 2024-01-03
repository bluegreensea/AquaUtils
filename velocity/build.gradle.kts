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
    maven("https://maven.elytrium.net/repo/") {
        name = "elytrium-repo"
    }
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    // Velocity use adventure 4.15.0-SNAPSHOT
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-snapshots-repo"
    }
}
dependencies {
    implementation(rootProject.project("common"))

    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    kapt("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-proxy:3.3.0-SNAPSHOT")
    val nettyVersion: String by project
    compileOnly("io.netty:netty-transport:$nettyVersion")

    val cloudVersion: String by project
    implementation("cloud.commandframework:cloud-velocity:$cloudVersion")
    compileOnly("com.electronwill.night-config:toml:3.6.7")

    compileOnly("net.elytrium.limboapi:api:1.1.18")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar")))) // VelocityGUI
    compileOnly("dev.simplix:protocolize-velocity:2.3.3")
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
