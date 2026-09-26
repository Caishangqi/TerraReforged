rootProject.name = "TerraReforged"


fun includeImmediateChildren(dir: File, type: String) {
    dir.walkTopDown().maxDepth(1).forEach {
        if (!it.isDirectory || !File(it, "build.gradle.kts").exists()) return@forEach
        val addonDir = it.relativeTo(file(".")).path.replace("/", ":").replace("\\", ":")
        logger.info("Including $type directory \"$addonDir\" as subproject.")
        include(addonDir)
    }
}

includeImmediateChildren(file("common/api"), "API")

includeImmediateChildren(file("common/implementation"), "implementation")

includeImmediateChildren(file("common/addons"), "addon")

// The 26.2 port delivers the Paper tree only. Allay, CLI, Fabric, Minestom, the merged jar, and the
// mixin platforms stay on disk but out of the build, so their toolchains cannot block a Paper compile.
// Re-include a platform here when a delivery explicitly names it.
include(":platforms:bukkit")
include(":platforms:bukkit:common")

// One module per set of Minecraft versions that share their internals. Adding a version means adding
// a directory here and a binding in NMSInitializer; nothing lists the modules by name.
includeImmediateChildren(file("platforms/bukkit/nms"), "Bukkit NMS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.solo-studios.ca/releases") {
            name = "Solo Studios"
        }
        maven("https://maven.solo-studios.ca/snapshots") {
            name = "Solo Studios"
        }
        maven("https://maven.fabricmc.net") {
            name = "Fabric Maven"
        }
        maven("https://maven.architectury.dev/") {
            name = "Architectury Maven"
        }
        maven("https://files.minecraftforge.net/maven/") {
            name = "Forge Maven"
        }
        maven("https://maven.quiltmc.org/repository/release/") {
            name = "Quilt"
        }
    }
}

// settings.gradle.kts
val isCiServer = System.getenv().containsKey("CI")
// Cache build artifacts, so expensive operations do not need to be re-computed
buildCache {
    local {
        isEnabled = !isCiServer
    }
}