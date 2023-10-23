import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("io.papermc.paperweight.userdev") version "1.3.7"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2" // Generates plugin.yml
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.projectlombok", "lombok", "1.18.22")
    annotationProcessor("org.projectlombok", "lombok", "1.18.22")
    paperweightDevelopmentBundle("org.purpurmc.purpur:dev-bundle:${project.property("api_version")}")
    compileOnly("co.aikar", "acf-paper", "0.5.1-SNAPSHOT")
    implementation(project(":lib"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    reobfJar {
        dependsOn(shadowJar)
    }

    shadowJar {
        minimize()
        archiveClassifier.set("")
        archiveBaseName.set("PluginLibTest")
    }

    runServer {
        runDirectory.set(file("${project.projectDir}/run"))
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}

bukkit {
    name = "PluginLibTest"
    version = "1.0.0"
    main = "com.alexsobiek.pluginlib.test.TestPlugin"
    apiVersion = "1.19"
    authors = listOf("Alex Sobiek")
}