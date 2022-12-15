dependencies {
    api("net.kyori:adventure-api:4.12.0")
}
blossom {
    replaceToken("\${version}", version, "src/main/java/bluesea/aquautils/common/Processor.java")
}
