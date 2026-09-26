import io.papermc.paperweight.userdev.attribute.Obfuscation

plugins {
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper") version Versions.Bukkit.runPaper
}

// A Paperweight project publishes both a reobf and a Mojang-mapped variant, and "shaded" carries no
// attribute that tells them apart. Paper 26.2 runs Mojang-mapped, so shade the unobfuscated one.
configurations.named("shaded") {
    attributes {
        attribute(Obfuscation.OBFUSCATION_ATTRIBUTE, objects.named(Obfuscation::class, Obfuscation.NONE))
    }
}

// Every module under platforms/bukkit/nms is a Minecraft version adapter, and all of them ship in the
// jar; NMSInitializer picks one at runtime. settings.gradle.kts includes them by walking that
// directory, so naming a module here would be a second place to remember when a version is added.
val nmsModules = project(":platforms:bukkit:nms").subprojects.map { it.path }

dependencies {
    // Required for :platforms:bukkit:runDevBundleServer task
    paperweight.paperDevBundle(Versions.Bukkit.paperDevBundle)

    shaded(project(":platforms:bukkit:common"))
    nmsModules.forEach { shaded(project(it)) }
    shaded("xyz.jpenilla", "reflection-remapper", Versions.Bukkit.reflectionRemapper)
}

tasks {
    shadowJar {
        relocate("io.papermc.lib", "com.dfsek.terra.lib.paperlib")
        relocate("com.google.common", "com.dfsek.terra.lib.google.common")
        relocate("org.apache.logging.slf4j", "com.dfsek.terra.lib.slf4j-over-log4j")
        exclude("org/slf4j/**")
        exclude("org/checkerframework/**")
        exclude("org/jetbrains/annotations/**")
        exclude("org/intellij/**")
        exclude("com/google/errorprone/**")
        exclude("com/google/j2objc/**")
        exclude("javax/**")
    }

    runServer {
        minecraftVersion(Versions.Bukkit.minecraft)
        dependsOn(shadowJar)
        pluginJars(shadowJar.get().archiveFile)

        downloadPlugins {
            modrinth("viaversion", "5.5.0")
            modrinth("viabackwards", "5.5.0")
        }
    }
}


addonDir(project.file("./run/plugins/TerraReforged/addons"), tasks.named("runServer").get())

// Optional, and untracked: a developer's own machine paths and ad-hoc tasks. A clone without it builds
// normally, so this file must never hold anything the build depends on.
rootProject.file("local.gradle").takeIf { it.isFile }?.let { apply(from = it) }
