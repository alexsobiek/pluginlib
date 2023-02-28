rootProject.name = "pluginlib"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}


include("lib", "test-plugin")
