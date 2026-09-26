/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.dfsek.terra.api.command.CommandSender;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.event.events.platform.CommandRegistrationEvent;
import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.banner.Banners;
import com.dfsek.terra.banner.PluginBanner;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;
import com.dfsek.terra.lang.Messages;
import com.dfsek.terra.log.TerraLog;
import com.dfsek.terra.bukkit.listeners.CommonListener;
import com.dfsek.terra.bukkit.util.PaperUtil;
import com.dfsek.terra.bukkit.util.VersionUtil;
import com.dfsek.terra.bukkit.world.BukkitAdapter;


public class TerraBukkitPlugin extends JavaPlugin {
    private static final Logger logger = LoggerFactory.getLogger(TerraBukkitPlugin.class);
    private final Map<String, com.dfsek.terra.api.world.chunk.generation.ChunkGenerator> generatorMap = new HashMap<>();
    private PlatformImpl platform;
    private AsyncScheduler asyncScheduler = this.getServer().getAsyncScheduler();

    private GlobalRegionScheduler globalRegionScheduler = this.getServer().getGlobalRegionScheduler();

    @Override
    public void onEnable() {
        installStyledLogging();
        printBanner();

        if(!doVersionCheck()) {
            return;
        }

        platform = NMSInitializer.init(this);
        if(platform == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        platform.getEventManager().callEvent(new PlatformInitializationEvent());

        try {
            PaperCommandManager<CommandSender> commandManager = getCommandSenderPaperCommandManager();

            platform.getEventManager().callEvent(new CommandRegistrationEvent(commandManager));

        } catch(Exception e) { // This should never happen.
            TerraLog.logger().send("platform.command-failure");
            logger.error("Errors occurred while registering commands.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(new CommonListener(platform), this); // Register master event listener
        PaperUtil.checkPaper(this);
    }

    /**
     * Binds the styled console logger before anything else logs. The display name comes from
     * {@code plugin.yml} rather than a constant, so the plugin is named in exactly one place.
     */
    private void installStyledLogging() {
        Audience console = getServer().getConsoleSender();
        Path langFile = getDataFolder().toPath().resolve(Messages.RESOURCE);
        Messages messages;
        try {
            messages = Messages.load(langFile, getClass().getClassLoader());
        } catch(IOException e) {
            // A broken language file must not stop a start. The defaults carry the same keys.
            messages = Messages.defaults();
            logger.error("Could not load {}; using the built-in messages.", langFile, e);
        }
        TerraLog.install(getPluginMeta().getName(), console::sendMessage, messages);
    }

    private void printBanner() {
        List<String> authors = getPluginMeta().getAuthors();
        new PluginBanner(Banners.TERRA_REFORGED, TerraLog.logger())
            .print(Map.of(
                "version", getPluginMeta().getVersion(),
                "authors", String.join(", ", authors),
                "platform", getServer().getName() + " " + getServer().getVersion(),
                "jvm", System.getProperty("java.vm.version", "unknown")));
    }

    @NotNull
    private PaperCommandManager<CommandSender> getCommandSenderPaperCommandManager() throws Exception {
        PaperCommandManager<CommandSender> commandManager = PaperCommandManager.builder(SenderMapper.create(
                BukkitAdapter::adapt,
                BukkitAdapter::adapt
            ))
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
            .buildOnEnable(this);

        commandManager.brigadierManager().setNativeNumberSuggestions(false);

        return commandManager;
    }

    public PlatformImpl getPlatform() {
        return platform;
    }

    @SuppressWarnings({ "deprecation", "AccessOfSystemProperties" })
    private boolean doVersionCheck() {
        TerraLog.logger().send("platform.server-version", Map.of(
            "minecraft", String.valueOf(VersionUtil.getMinecraftVersionInfo()),
            "implementation", Bukkit.getServer().getName()));

        if(!VersionUtil.getSpigotVersionInfo().isSpigot())
            TerraLog.logger().send("platform.bukkit-server");

        if(!VersionUtil.getSpigotVersionInfo().isPaper())
            TerraLog.logger().send("platform.spigot-server");

        if(VersionUtil.getSpigotVersionInfo().isMohist()) {
            if(System.getProperty("IKnowMohistCausesLotsOfIssuesButIWillUseItAnyways") == null) {
                Runnable runnable = () -> { // scary big block of text
                    logger.error("""
                                 .----------------------------------------------------------------------------------.
                                 |                                                                                  |
                                 |                                ⚠ !! Warning !! ⚠                                 |
                                 |                                                                                  |
                                 |                         You are currently using Mohist.                          |
                                 |                                                                                  |
                                 |                                Do not use Mohist.                                |
                                 |                                                                                  |
                                 |   The concept of combining the rigid Bukkit platform, which assumes a 100%       |
                                 |   Vanilla server, with the flexible Forge platform, which allows changing        |
                                 |   core components of the game, simply does not work. These platforms are         |
                                 |   incompatible at a conceptual level, the only way to combine them would         |
                                 |   be to make incompatible changes to both. As a result, Mohist's Bukkit          |
                                 |   API implementation is not compliant. This will cause many plugins to           |
                                 |   break. Rather than fix their platform, Mohist has chosen to distribute         |
                                 |   unofficial builds of plugins they deem to be "fixed". These builds are not     |
                                 |   "fixed", they are simply hacked together to work with Mohist's half-baked      |
                                 |   Bukkit implementation. To distribute these as "fixed" versions implies that:   |
                                 |       - These builds are endorsed by the original developers. (They are not)     |
                                 |       - The issue is on the plugin's end, not Mohist's. (It is not. The issue    |
                                 |       is that Mohist chooses to not create a compliant Bukkit implementation)    |
                                 |   Please, do not use Mohist. It causes issues with most plugins, and rather      |
                                 |   than fixing their platform, Mohist has chosen to distribute unofficial         |
                                 |   hacked-together builds of plugins, calling them "fixed". If you want           |
                                 |   to use a server API with Forge mods, look at the Sponge project, an            |
                                 |   API that is designed to be implementation-agnostic, with first-party           |
                                 |   support for the Forge mod loader. You are bound to encounter issues if         |
                                 |   you use Terra with Mohist. We will provide NO SUPPORT for servers running      |
                                 |   Mohist. If you wish to proceed anyways, you can add the JVM System Property    |
                                 |   "IKnowMohistCausesLotsOfIssuesButIWillUseItAnyways" to enable the plugin. No   |
                                 |   support will be provided for servers running Mohist.                           |
                                 |                                                                                  |
                                 |                   Because of this **TERRA HAS BEEN DISABLED**.                   |
                                 |                    Do not come ask us why it is not working.                     |
                                 |                                                                                  |
                                 |----------------------------------------------------------------------------------|
                                 """.strip());
                };
                runnable.run();
                asyncScheduler.runDelayed(this, task -> runnable.run(), 200L, TimeUnit.SECONDS);
                // Bukkit.shutdown(); // we're not *that* evil
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            } else {
                logger.warn("""
                            You are using Mohist, so we will not give you any support for issues that may arise.
                            Since you enabled the "IKnowMohistCausesLotsOfIssuesButIWillUseItAnyways" flag, we won't disable Terra. But be warned.
                            
                            > I felt a great disturbance in the JVM, as if millions of plugins suddenly cried out in stack traces and were suddenly silenced.
                            > I fear something terrible has happened.
                            > - Astrash
                            """.strip());
            }
        }
        return true;
    }

    @Override
    public @Nullable
    ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        if(id == null || id.trim().isEmpty()) { return null; }
        return new BukkitChunkGeneratorWrapper(generatorMap.computeIfAbsent(worldName, name -> {
            ConfigPack pack = platform.getConfigRegistry().getByID(id).orElseThrow(
                () -> new IllegalArgumentException("No such config pack \"" + id + "\""));
            return pack.getGeneratorProvider().newInstance(pack);
        }), platform.getRawConfigRegistry().getByID(id).orElseThrow(), platform.getWorldHandle().air());
    }

    public AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    public GlobalRegionScheduler getGlobalRegionScheduler() {
        return globalRegionScheduler;
    }
}
