import org.gradle.api.file.DuplicatesStrategy

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.ququla89"
version = "1.0.0"

val pluginVersion = version.toString()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

runPaper.disablePluginJarDetection()

tasks {
    shadowJar {
        archiveBaseName.set("HeadHunt")
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        relocate("kotlin", "com.ququla89.headhunt.lib.kotlin")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to pluginVersion)
        }
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.4")
        pluginJars.from(shadowJar)
        disablePluginRemapping()
    }
}
