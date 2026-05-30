plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.0.0-rc2"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.1")
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/craft-engine-paper-plugin-26.5.3.jar"))
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/[C农作物]CustomCrops-3.6.50.jar"))
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/[W创世神]worldedit-bukkit-7.4.3.jar"))
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/[W世界保护]worldguard-bukkit-7.0.16.jar"))
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/0[R领地]Residence6.0.1.6.jar"))
    compileOnly(files("C:/Users/Administrator/Desktop/1.21.11server/Minecraft/Servers/Survival/plugins/[C前置]CMILib1.5.9.5.jar"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
