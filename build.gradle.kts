import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion apply false
    id("net.kyori.blossom") version "2.1.0" apply false
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("xyz.jpenilla.run-velocity") version "2.3.1" apply false
    val fabricLoomVersion: String by System.getProperties()
    id("fabric-loom") version fabricLoomVersion apply false
    checkstyle
}
allprojects {
    repositories {
        mavenCentral()
    }
}
val ktlint: Configuration by configurations.creating
dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.6.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "checkstyle")
    checkstyle {
        toolVersion = "10.17.0"
        configFile = rootProject.file("checkstyle.xml")
        maxErrors = 0
        maxWarnings = 0
    }
    base {
        archivesName.set("${rootProject.name}-${project.name.capitalized()}")
    }
    val modVersion: String by project
    version = modVersion
    val mavenGroup: String by project
    group = mavenGroup
    dependencies {
        val kotlinVersion: String by System.getProperties()
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

        val cloudVersion: String by project
        val cloudMinecraftVersion: String by project
        val cloudTranslationsVersion: String by project
        implementation("org.incendo:cloud-core:$cloudVersion")
        implementation("org.incendo:cloud-brigadier:$cloudMinecraftVersion")
        implementation("org.incendo:cloud-minecraft-extras:$cloudMinecraftVersion")
        implementation("org.incendo:cloud-translations-core:$cloudTranslationsVersion")
        // implementation("org.incendo:cloud-translations-minecraft-extras:$cloudTranslationsVersion")
    }
    val javaVersion = JavaVersion.VERSION_21
    tasks {
        val ktlintCheck by registering(JavaExec::class) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Check Kotlin code style."
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            args("src/**/*.kt", "**/*.kts", "!**/build/**")
        }
        withType<JavaCompile> {
            dependsOn(ktlintCheck)
            options.encoding = "UTF-8"
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
            options.release = javaVersion.toString().toInt()
        }
        jar { from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } } }
    }
    java {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion.toString())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
