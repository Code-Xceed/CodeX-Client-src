plugins {
    id("fabric-loom") version "1.9.2"
    `maven-publish`
}

val modVersion = providers.gradleProperty("mod_version").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val archiveName = providers.gradleProperty("archives_base_name").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val yarnMappings = providers.gradleProperty("yarn_mappings").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricVersion = providers.gradleProperty("fabric_version").get()

version = modVersion
group = mavenGroup

base {
    archivesName.set(archiveName)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val resourceProperties = mapOf(
        "version" to modVersion,
        "minecraftVersion" to minecraftVersion,
        "loaderVersion" to loaderVersion
    )
    inputs.properties(resourceProperties)
    filesMatching("fabric.mod.json") {
        expand(resourceProperties)
    }
}
