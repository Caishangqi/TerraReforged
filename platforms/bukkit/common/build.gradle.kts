repositories {
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    shadedApi(project(":common:implementation:base"))

    compileOnly("io.papermc.paper", "paper-api", Versions.Bukkit.paper)

    // OraxenBlockStateTest needs org.bukkit.block.data.BlockData at test runtime, and compileOnly is
    // not on the test classpath.
    testImplementation("io.papermc.paper", "paper-api", Versions.Bukkit.paper)

    compileOnly("org.mvplugins.multiverse.core", "multiverse-core", Versions.Bukkit.multiverse)

    compileOnly("io.th0rgal", "oraxen", Versions.Bukkit.oraxen)

    compileOnly("net.momirealms", "craft-engine-core", Versions.Bukkit.craftEngine)
    compileOnly("net.momirealms", "craft-engine-bukkit", Versions.Bukkit.craftEngine)

    shadedApi("io.papermc", "paperlib", Versions.Bukkit.paperLib)

    shadedApi("com.google.guava", "guava", Versions.Libraries.Internal.guava)

    shadedApi("org.incendo", "cloud-paper", Versions.Bukkit.cloud)
}
