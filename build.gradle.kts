plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22"
}

group = "verymc.top"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

paperweight {
    reobfArtifactConfiguration =
        io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

tasks {
    processResources {
        val projectVersion = project.version

        filesMatching("plugin.yml") {
            expand(mapOf("version" to projectVersion))
        }
    }
}