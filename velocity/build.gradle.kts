import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    id("com.github.johnrengelman.shadow").version("7.1.2")
}
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    api(rootProject.project("common"))

    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("org.yaml:snakeyaml:1.33")

    implementation("net.kyori:adventure-api:4.12.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.12.0")
    compileOnly("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
}
blossom {
    replaceToken("\${version}", version, "src/main/java/bluesea/aquautils/AquaUtilsVelocity.java")
}
tasks {
    shadowJar {
        archiveFileName.set("${archivesName.get()}-shadowJar.jar")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
