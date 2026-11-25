// Build script for Fabric nodes. Stonecutter runs it once per `<minecraft>-fabric` node.
plugins {
    // Unobfuscated Loom, for Minecraft 26.1+ only. No remapping: plain `implementation` and `jar`.
    id("net.fabricmc.fabric-loom") version "1.19.4"
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

val mcVersion: String = sc.current.version
val modId: String = sc.properties["mod.id"]
val modName: String = sc.properties["mod.name"]
val modVersion: String = sc.properties["mod.version"]
val modSide: String = sc.properties["mod.side"]
val loaderVersion: String = sc.properties["deps.fabric_loader"]
val fabricApiVersion: String = sc.properties["deps.fabric_api"]
val gameVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

require(modSide == "server" || modSide == "client") {
    "mod.side must be \"server\" or \"client\", but was \"$modSide\""
}

// Jar name: <mod id>-fabric-<mod version>+mc<minecraft version>.jar
// Do not set `group`: nodes must stay distinguishable by project path only.
val fullVersion = "$modVersion+mc$mcVersion"
version = fullVersion
base.archivesName = "$modId-fabric"

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
}

loom {
    runConfigs.all {
        // One shared run directory instead of one per node.
        runDirectory = layout.settingsDirectory.dir("run").asFile
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 25
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to modName,
            "version" to fullVersion,
            "description" to sc.properties["mod.description"],
            "repository" to sc.properties["mod.repository"],
            // A "server" mod must also load on clients, otherwise it is missing from the
            // integrated server in singleplayer. Fabric's "server" means dedicated server only.
            "environment" to if (modSide == "client") "client" else "*",
            "minecraft" to sc.properties["mod.mc_compat"],
            "loader" to loaderVersion,
            "fabric_api" to fabricApiVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    jar {
        // Local copy: a lambda that captures a script-level val breaks the configuration cache.
        val licenseSuffix = modId
        from(layout.settingsDirectory.file("LICENSE")) { rename { "${it}_$licenseSuffix" } }
    }
}

// Publishing. `publishMods` uploads for real only on CI; anywhere else it is a dry run
// that writes what it would upload to this node's build directory.
publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    displayName = "$modName $modVersion (Fabric $mcVersion)"
    version = "$fullVersion-fabric"
    type = when {
        "alpha" in modVersion -> ALPHA
        "beta" in modVersion -> BETA
        else -> STABLE
    }
    modLoaders.add("fabric")
    // Written by the release workflow from the matching CHANGELOG.md section.
    changelog = providers.fileContents(layout.settingsDirectory.file("build/release-notes.md"))
        .asText.orElse("See CHANGELOG.md.")
    dryRun = !providers.environmentVariable("CI").isPresent

    val modrinthId = sc.properties.getOrNull("publish.modrinth_id")
    if (!modrinthId.isNullOrBlank()) {
        modrinth {
            projectId = modrinthId
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            minecraftVersions.addAll(gameVersions)
            // This mod has an optional client part (syncs item components to modded clients).
            environment = if (modSide == "client") CLIENT_ONLY else SERVER_ONLY_CLIENT_OPTIONAL
            requires("fabric-api")
        }
    }

    val curseforgeId = sc.properties.getOrNull("publish.curseforge_id")
    if (!curseforgeId.isNullOrBlank()) {
        curseforge {
            projectId = curseforgeId
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            minecraftVersions.addAll(gameVersions)
            javaVersions.add(JavaVersion.VERSION_25)
            // A "server" mod is also installed on clients for singleplayer use.
            client = true
            server = modSide == "server"
            requires("fabric-api")
        }
    }
}
