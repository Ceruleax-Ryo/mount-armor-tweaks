pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
    }
}

plugins {
    // https://stonecutter.kikugie.dev/blog/changes/0.10 (needs Java 25 and Gradle 9.7+)
    id("dev.kikugie.stonecutter") version "0.10"
}

stonecutter {
    create(rootProject) {
        /**
         * Registers one node per loader for a Minecraft version.
         * Node `<minecraft>-<loader>` lives in `versions/<minecraft>-<loader>/`
         * and is built by `build.<loader>.gradle.kts`.
         */
        fun match(minecraft: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$minecraft-$loader", minecraft).buildscript("build.$loader.gradle.kts")
            }
        }

        match("26.3", "fabric")

        // The node whose sources are checked into src/.
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "mount-armor-tweaks"
