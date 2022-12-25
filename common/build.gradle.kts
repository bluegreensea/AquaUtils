dependencies {
    val kyoriVersion: String by project
    api("net.kyori:adventure-api:$kyoriVersion")
}
blossom {
    replaceToken("\${version}", version, "src/main/java/bluesea/aquautils/common/Controller.java")
}
