import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    id("xyz.jpenilla.run-velocity").version("2.0.1")
    id("com.github.johnrengelman.shadow").version("7.1.2")
}
val velocityVersion: String by project
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    api(rootProject.project("common"))

    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("org.yaml:snakeyaml:1.33")

    val kyoriVersion: String by project
    implementation("net.kyori:adventure-api:$kyoriVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$kyoriVersion")
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")
}
blossom {
    replaceToken("\${version}", version, "src/main/java/bluesea/aquautils/AquaUtilsVelocity.java")
}
tasks {
    runVelocity {
        velocityVersion(velocityVersion)
    }
    shadowJar {
        archiveFileName.set("${archivesName.get()}-shadowJar.jar")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
