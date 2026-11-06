import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "dev.zorsh"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2025.07.14-1.21.7")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
}

kotlin {
    jvmToolchain(25)
}

tasks.shadowJar {
    enabled = false
}

val shadowJarGame = tasks.register<ShadowJar>("shadowJarGame") {
    group = "shadow"
    description = "Собирает Shadow JAR для Game сервера"

    from(sourceSets.main.get().output) {
        include("dev/zorsh/neverland/game/**")
    }
    configurations = tasks.shadowJar.get().configurations

    archiveBaseName.set("server-game")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "dev.zorsh.neverland.game.MainKt"
    }
}

val shadowJarLobby = tasks.register<ShadowJar>("shadowJarLobby") {
    group = "shadow"
    description = "Собирает Shadow JAR для Lobby сервера"

    from(sourceSets.main.get().output) {
        include("dev/zorsh/neverland/lobby/**")
    }
    configurations = tasks.shadowJar.get().configurations

    archiveBaseName.set("server-lobby")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "dev.zorsh.neverland.lobby.MainKt"
    }
}

val shadowJarLauncher = tasks.register<ShadowJar>("shadowJarLauncher") {
    group = "shadow"
    description = "Собирает Shadow JAR для Launcher"

    from(sourceSets.main.get().output) {
        include("dev/zorsh/neverland/launcher/**")
    }
    configurations = tasks.shadowJar.get().configurations

    archiveBaseName.set("server-launcher")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "dev.zorsh.neverland.launcher.MainKt"
    }
}

tasks.build {
    dependsOn(shadowJarGame, shadowJarLobby, shadowJarLauncher)
}

tasks.register<Zip>("makeResourcePack") {
    archiveFileName.set("Neverland.zip")
    destinationDirectory.set(layout.buildDirectory.dir("resourcepack"))

    from("resourcepack") {
        include("**/*.*")
    }
}