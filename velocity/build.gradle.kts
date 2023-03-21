plugins {
    kotlin("kapt")
    id("xyz.jpenilla.run-velocity").version("2.0.1")
    id("com.github.johnrengelman.shadow").version("8.0.0")
}
val velocityVersion: String by project
repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    api(rootProject.project("common"))

    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("org.yaml:snakeyaml:2.0")

    val adventureVersion: String by project
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    kapt("com.velocitypowered:velocity-api:$velocityVersion")
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
        archiveClassifier.set("shadowJar")
        archiveVersion.set("")
    }
}
artifacts {
    archives(tasks.shadowJar)
}
