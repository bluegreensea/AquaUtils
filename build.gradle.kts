plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    checkstyle
    id("net.kyori.blossom").version("1.3.1")
}
val ktlint: Configuration by configurations.creating
allprojects {
    repositories {
        mavenCentral()
    }
}
dependencies {
    ktlint("com.pinterest:ktlint:0.49.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "checkstyle")
    apply(plugin = "net.kyori.blossom")
    checkstyle {
        toolVersion = "10.5.0"
        configFile = rootProject.file("checkstyle.xml")
        maxErrors = 0
        maxWarnings = 0
    }
    base {
        val archivesBaseName: String by project
        archivesName.set("$archivesBaseName-${project.name}")
    }
    val modVersion: String by project
    version = modVersion
    val mavenGroup: String by project
    group = mavenGroup
    repositories {
        maven("https://libraries.minecraft.net")
    }
    dependencies {
        val kotlinVersion: String by System.getProperties()
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

        compileOnly("com.mojang:brigadier:1.0.18")
        val cloudVersion: String by project
        implementation("cloud.commandframework:cloud-core:$cloudVersion")
        implementation("cloud.commandframework:cloud-brigadier:$cloudVersion")
    }
    tasks {
        val ktlintCheck by creating(JavaExec::class) {
            inputs.files(project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt")))
            outputs.dir("${project.buildDir}/reports/ktlint/")
            description = "Check Kotlin code style."
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            args = listOf("src/**/*.kt", "**/*.kts")
        }
        val javaVersion = JavaVersion.VERSION_17
        withType<JavaCompile> {
            dependsOn(ktlintCheck)
            options.encoding = "UTF-8"
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
            options.release.set(javaVersion.toString().toInt())
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { jvmTarget = javaVersion.toString() }
            // sourceCompatibility = javaVersion.toString()
            // targetCompatibility = javaVersion.toString()
        }
        jar { from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } } }
        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            withSourcesJar()
        }
    }
}
