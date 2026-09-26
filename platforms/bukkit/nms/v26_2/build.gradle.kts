// The NMS adapter for Minecraft 26.2. One module per set of Minecraft versions that share their
// internals; NMSInitializer binds a version to this module's package name and loads
// com.dfsek.terra.bukkit.nms.v26_2.NMSPlatform reflectively, because common cannot see it.
//
// A later version is a sibling directory: settings.gradle.kts includes it by walking this directory
// and platforms/bukkit shades whatever it found, so only the binding in NMSInitializer is a decision.

plugins {
    id("io.papermc.paperweight.userdev")
}

dependencies {
    api(project(":platforms:bukkit:common"))
    paperweight.paperDevBundle(Versions.Bukkit.paperDevBundle)
    implementation("xyz.jpenilla", "reflection-remapper", Versions.Bukkit.reflectionRemapper)
}

tasks.test {
    // NMSBindingTest checks that the version this module is compiled against is a version it claims to
    // serve. The pin lives in Versions.kt and the claim lives in NMSInitializer.BINDINGS, with nothing
    // between them, so the test needs the pin handed to it.
    systemProperty("terra.test.minecraftVersion", Versions.Bukkit.minecraft)
}
