import com.github.monosoul.yadegrap.DelombokTask

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.github.monosoul.yadegrap") version "1.0.0"
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://maven.alexsobiek.com/snapshots")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.projectlombok", "lombok", "1.18.22")
    annotationProcessor("org.projectlombok", "lombok", "1.18.22")
    compileOnly("org.purpurmc.purpur", "purpur-api", project.property("api_version") as String)
    implementation("com.alexsobiek", "nexus", "main-SNAPSHOT")
    implementation("co.aikar", "acf-paper", "0.5.1-SNAPSHOT")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }

    build {
        dependsOn(shadowJar)
    }


    shadowJar {
        archiveClassifier.set("")
        relocate("com.alexsobiek.nexus", "com.alexsobiek.pluginlib.dependencies.nexus")
        relocate("co.aikar.commands", "com.alexsobiek.pluginlib.dependencies.acf")
    }


    val delombok = "delombok"(DelombokTask::class)

    "javadoc"(Javadoc::class) {
        dependsOn(delombok)
        setSource(delombok)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
