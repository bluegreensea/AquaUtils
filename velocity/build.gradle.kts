plugins {
    id("org.jetbrains.kotlin.kapt")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-velocity")
}
val velocityVersion: String by project
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://maven.elytrium.net/repo/") {
        name = "elytrium-repo"
        content {
            includeGroupAndSubgroups("net.elytrium")
        }
    }
}
dependencies {
    implementation(project(":common"))

    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("com.google.guava:guava:33.2.0-jre")
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-brigadier:1.0.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-proxy:3.3.0.1-SNAPSHOT")
    val nettyVersion: String by project
    compileOnly("io.netty:netty-transport:$nettyVersion")

    val cloudMinecraftVersion: String by project
    val cloudTranslationsVersion: String by project
    implementation("org.incendo:cloud-velocity:$cloudMinecraftVersion")
    implementation("org.incendo:cloud-translations-velocity:$cloudTranslationsVersion")
    compileOnly("com.electronwill.night-config:toml:3.7.1")

    compileOnly("net.elytrium.limboapi:api:1.1.26")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar")))) // VelocityGUI
    compileOnly("dev.simplix:protocolize-velocity:2.4.1")
    compileOnly("com.github.retrooper:packetevents-velocity:2.7.0")
}
tasks {
    runVelocity {
        velocityVersion(velocityVersion)
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    }
    shadowJar {
        archiveClassifier.set("with-dependencies")
        archiveVersion.set("")
        exclude("META-INF/maven/**")
        val dependencyDir = "${project.group}.dependencies"
        relocate("org.incendo.cloud", "$dependencyDir.cloud")
        relocate("io.leangen.geantyref", "$dependencyDir.typetoken")
        relocate("org.jsoup", "$dependencyDir.jsoup")
    }
    register<Copy>("distRun") {
        dependsOn(shadowJar)
        group = "shadow"
        from(shadowJar.get().archiveFile.get())
        into("run/plugins")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
