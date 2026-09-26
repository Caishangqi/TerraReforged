package com.dfsek.terra.bukkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.dfsek.terra.bukkit.util.VersionUtil;


/**
 * Chooses the NMS adapter module for the running server and constructs its platform.
 *
 * <p>This is the whole version SPI. The adapters live under {@code platforms/bukkit/nms}, one module
 * per set of Minecraft versions that share their internals, and this layer reaches them only by name:
 * an adapter compiles against the Paper Dev Bundle and this module does not, which is what keeps
 * {@code net.minecraft} out of the common Bukkit code rather than a convention that has to be policed.
 *
 * <p>Supporting a new Minecraft version is a new module directory and an entry in {@link #BINDINGS}.
 */
public interface NMSInitializer {
    /**
     * One entry per adapter module, newest first.
     *
     * <p>A module is bound to the versions it serves rather than named after one. A Minecraft patch
     * release usually leaves the internals alone, so deriving the module name from the version string
     * would force a new module for every string the server can report.
     *
     * <p>The ordering is load bearing: the bypass flag below has to construct something on a version
     * with no binding, and the newest adapter is the only defensible guess.
     *
     * <p>{@code MinecraftVersionInfo} drops the patch segment when the server reports none, so 26.2
     * renders as {@code v26.2} rather than {@code v26.2.0}.
     */
    List<Binding> BINDINGS = List.of(
        new Binding("v26_2", List.of("v26.2"))
    );

    static PlatformImpl init(TerraBukkitPlugin plugin) {
        Logger logger = LoggerFactory.getLogger(NMSInitializer.class);

        // This refusal is a block of lines rather than one prefixed line, so it stays on SLF4J. The name
        // comes from plugin.yml so the block cannot contradict a rename.
        String name = plugin.getPluginMeta().getName();
        String minecraftVersion = VersionUtil.getMinecraftVersionInfo().toString();
        Binding binding = bindingFor(minecraftVersion).orElse(null);

        if(binding == null) {
            logger.error("You are running your server on Minecraft version {} which is not supported by this version of {}.",
                minecraftVersion, name);

            String bypassKey = "IKnowThereAreNoNMSBindingsFor" + minecraftVersion.replace(".", "_") + "ButIWillProceedAnyway";
            if(System.getProperty(bypassKey) == null) {
                logger.error("Because of this **{} HAS BEEN DISABLED**.", name.toUpperCase(Locale.ROOT));
                logger.error("Do not come ask us why it is not working.");
                logger.error("If you wish to proceed anyways, you can add the JVM System Property \"{}\" to enable the plugin.", bypassKey);
                return null;
            }

            logger.error("");
            logger.error("");
            for(int i = 0; i < 20; i++) {
                logger.error("PROCEEDING WITH AN EXISTING {} WORLD WILL RESULT IN CORRUPTION!!!", name.toUpperCase(Locale.ROOT));
            }
            logger.error("");
            logger.error("");
            logger.error("We will not give you any support for issues that may arise.");
            logger.error("Since you enabled the \"{}\" flag, we won't disable {}. But be warned.", bypassKey, name);

            binding = BINDINGS.get(0);
            logger.error("Falling back to the newest bindings {} has, {}. They were not written for this server.",
                name, binding.module());
        }

        logger.info("Using the {} NMS bindings for Minecraft {}.", binding.module(), minecraftVersion);
        return constructPlatform(plugin, binding);
    }

    /**
     * The adapter that serves a Minecraft version, or empty when none does.
     *
     * <p>Takes the version rather than reading it from the server so that it can be exercised without
     * one. A binding that compiles and is never selected is the failure this is here to catch.
     */
    static Optional<Binding> bindingFor(String minecraftVersion) {
        return BINDINGS.stream()
            .filter(binding -> binding.minecraftVersions().contains(minecraftVersion))
            .findFirst();
    }

    private static PlatformImpl constructPlatform(TerraBukkitPlugin plugin, Binding binding) {
        try {
            Class<?> platformClass = Class.forName(binding.platformClassName());
            return (PlatformImpl) platformClass
                .getConstructor(TerraBukkitPlugin.class)
                .newInstance(plugin);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException("Error initializing the " + binding.module() + " NMS bindings. Report this to Terra.", e);
        }
    }

    /**
     * An adapter module and the Minecraft versions it serves.
     *
     * @param module            the module's package under {@code com.dfsek.terra.bukkit.nms}, and its
     *                          directory under {@code platforms/bukkit/nms}
     * @param minecraftVersions the versions as {@code MinecraftVersionInfo} renders them
     */
    record Binding(String module, List<String> minecraftVersions) {
        /**
         * The adapter's entry point, a {@link PlatformImpl} subclass taking a {@link TerraBukkitPlugin}.
         * Named rather than referenced because this module cannot see it.
         */
        public String platformClassName() {
            return NMSInitializer.class.getPackageName() + ".nms." + module + ".NMSPlatform";
        }
    }
}
