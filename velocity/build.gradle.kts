plugins {
    kotlin("kapt")
    id("xyz.jpenilla.run-velocity").version("2.0.1")
    id("com.github.johnrengelman.shadow").version("8.1.1")
}
val velocityVersion: String by project
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
}
dependencies {
    api(rootProject.project("common"))

    compileOnly("com.google.code.gson:gson:2.10.1")
    val adventureVersion: String by project
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    kapt("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion")

    val cloudVersion: String by project
    implementation("cloud.commandframework:cloud-velocity:$cloudVersion")
    compileOnly("com.electronwill.night-config:toml:3.6.6")

    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly("dev.simplix:protocolize-velocity:2.2.6")
}
blossom {
    replaceToken("\\\${version}", version, "src/main/java/bluesea/aquautils/AquaUtilsVelocity.kt")
}
tasks {
    register<Copy>("distRun") {
        dependsOn(shadowJar)
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
