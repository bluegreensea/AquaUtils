repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}
dependencies {
    compileOnly("com.google.code.gson:gson:2.10.1")

    val adventureVersion: String by project
    api("net.kyori:adventure-api:$adventureVersion")
    api("net.kyori:adventure-text-serializer-gson:$adventureVersion")

    compileOnly("org.slf4j:slf4j-api:2.0.6")
    compileOnly("org.apache.logging.log4j:log4j-api:2.19.0")

    implementation("org.jsoup:jsoup:1.15.4")

    compileOnly("org.yaml:snakeyaml:2.0")
    val velocityVersion: String by project
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
}
blossom {
    replaceToken("\\\${version}", version, "src/main/java/bluesea/aquautils/common/Controller.java")
}
