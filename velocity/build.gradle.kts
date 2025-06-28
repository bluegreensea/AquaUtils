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
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("net.elytrium")
        }
    }
}
dependencies {
    implementation(project(":common")) {
        exclude("com.electronwill.night-config")
    }

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

    compileOnly("net.elytrium.limboapi:api:1.1.26")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar")))) // VelocityGUI
    compileOnly("dev.simplix:protocolize-velocity:2.4.3")
    compileOnly("com.github.retrooper:packetevents-velocity:2.7.0")
}
tasks {
    runVelocity {
        velocityVersion(velocityVersion)
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    }
    shadowJar {
        archiveClassifier.set("shadow")
        exclude("META-INF/maven/**")
        val dependencyDir = "${project.group}.dependencies"
        relocate("org.incendo.cloud", "$dependencyDir.cloud")
        relocate("io.leangen.geantyref", "$dependencyDir.typetoken")
        relocate("org.jsoup", "$dependencyDir.jsoup")
    }
    val dist by registering(Copy::class) {
        dependsOn(build, shadowJar)
        group = "shadow"
        from(shadowJar.get().archiveFile.get())
        into(shadowJar.get().archiveFile.get().asFile.parent)
        rename { name ->
            name.replace("-${shadowJar.get().archiveVersion.get()}", "")
        }
    }
    register<Copy>("distRun") {
        dependsOn(dist)
        group = "shadow"
        from(
            shadowJar.get().archiveFile.get().asFile.let {
                "${it.parent}/${it.name.replace("-${shadowJar.get().archiveVersion.get()}", "")}"
            }
        )
        into("run/plugins")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
