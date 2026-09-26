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

package com.dfsek.terra;

import com.dfsek.tectonic.api.TypeRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dfsek.terra.addon.BootstrapAddonLoader;
import com.dfsek.terra.addon.DependencySorter;
import com.dfsek.terra.addon.EphemeralAddon;
import com.dfsek.terra.addon.InternalAddon;
import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.addon.bootstrap.BootstrapAddonClassLoader;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.MetaPack;
import com.dfsek.terra.api.config.PluginConfig;
import com.dfsek.terra.api.event.EventManager;
import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.inject.Injector;
import com.dfsek.terra.api.inject.impl.InjectorImpl;
import com.dfsek.terra.api.profiler.Profiler;
import com.dfsek.terra.api.registry.CheckedRegistry;
import com.dfsek.terra.api.registry.Registry;
import com.dfsek.terra.api.registry.key.StringIdentifiable;
import com.dfsek.terra.api.util.generic.pair.Pair;
import com.dfsek.terra.api.util.mutable.MutableBoolean;
import com.dfsek.terra.api.util.reflection.TypeKey;
import com.dfsek.terra.config.GenericLoaders;
import com.dfsek.terra.config.PluginConfigImpl;
import com.dfsek.terra.event.EventManagerImpl;
import com.dfsek.terra.log.TerraLog;
import com.dfsek.terra.profiler.ProfilerImpl;
import com.dfsek.terra.registry.CheckedRegistryImpl;
import com.dfsek.terra.registry.LockedRegistryImpl;
import com.dfsek.terra.registry.OpenRegistryImpl;
import com.dfsek.terra.registry.master.ConfigRegistry;
import com.dfsek.terra.registry.master.ConfigRegistry.PackLoadFailuresException;
import com.dfsek.terra.registry.master.MetaConfigRegistry;


/**
 * Skeleton implementation of {@link Platform}
 * <p>
 * Implementations must invoke {@link #load()} in their constructors.
 */
public abstract class AbstractPlatform implements Platform {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPlatform.class);

    private static final MutableBoolean LOADED = new MutableBoolean(false);
    private static final String moonrise = "Moonrise";
    private final EventManager eventManager = new EventManagerImpl();
    private final ConfigRegistry configRegistry = new ConfigRegistry();
    private final MetaConfigRegistry metaConfigRegistry = new MetaConfigRegistry();
    private final CheckedRegistry<ConfigPack> checkedConfigRegistry = new CheckedRegistryImpl<>(configRegistry);
    private final CheckedRegistry<MetaPack> checkedMetaConfigRegistry = new CheckedRegistryImpl<>(metaConfigRegistry);
    private final Profiler profiler = new ProfilerImpl();
    private final GenericLoaders loaders = new GenericLoaders(this);
    private final PluginConfigImpl config = new PluginConfigImpl();
    private final CheckedRegistry<BaseAddon> addonRegistry = new CheckedRegistryImpl<>(new OpenRegistryImpl<>(TypeKey.of(BaseAddon.class)));
    private final Registry<BaseAddon> lockedAddonRegistry = new LockedRegistryImpl<>(addonRegistry);

    public static int getGenerationThreadsWithReflection(String className, String fieldName, String project) {
        try {
            Class aClass = Class.forName(className);
            int threads = aClass.getField(fieldName).getInt(null);
            logger.info("{} found, setting {} generation threads.", project, threads);
            return threads;
        } catch(ClassNotFoundException e) {
            logger.info("{} not found.", project);
        } catch(NoSuchFieldException e) {
            logger.warn("{} found, but {} field not found this probably means {0} has changed its code and " +
                        "Terra has not updated to reflect that.", project, fieldName);
        } catch(IllegalAccessException e) {
            logger.error("Failed to access {} field in {}, assuming 1 generation thread.", fieldName, project, e);
        }
        return 0;

    }

    public static int getMoonriseGenerationThreadsWithReflection() {
        try {
            Class<?> prioritisedThreadPoolClazz = Class.forName("ca.spottedleaf.concurrentutil.executor.thread.PrioritisedThreadPool");
            Method getCoreThreadsMethod = prioritisedThreadPoolClazz.getDeclaredMethod("getCoreThreads");
            getCoreThreadsMethod.setAccessible(true);
            Class<?> moonriseCommonClazz = Class.forName("ca.spottedleaf.moonrise.common.util.MoonriseCommon");
            Object pool = moonriseCommonClazz.getDeclaredField("WORKER_POOL").get(null);
            int threads = ((Thread[]) getCoreThreadsMethod.invoke(pool)).length;
            logger.info("{} found, setting {} generation threads.", moonrise, threads);
            return threads;
        } catch(ClassNotFoundException e) {
            logger.info("{} not found.", moonrise);
        } catch(NoSuchMethodException | NoSuchFieldException e) {
            logger.warn("{} found, but field/method not found this probably means {0} has changed its code and " +
                        "Terra has not updated to reflect that.", moonrise);
        } catch(IllegalAccessException | InvocationTargetException e) {
            logger.error("Failed to access thread values in {}, assuming 1 generation thread.", moonrise, e);
        }
        return 0;
    }

    public ConfigRegistry getRawConfigRegistry() {
        return configRegistry;
    }

    public MetaConfigRegistry getRawMetaConfigRegistry() {
        return metaConfigRegistry;
    }

    protected Iterable<BaseAddon> platformAddon() {
        return Collections.emptySet();
    }

    protected InternalAddon load() {
        if(LOADED.get()) {
            throw new IllegalStateException(
                "Someone tried to initialize Terra, but Terra has already initialized. This is most likely due to a broken platform " +
                "implementation, or a misbehaving mod.");
        }
        LOADED.set(true);

        TerraLog.logger().send("platform.initializing");

        try(InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            TerraLog.logger().send("platform.config-loading");
            File configFile = new File(getDataFolder(), "config.yml");
            if(!configFile.exists()) {
                TerraLog.logger().send("platform.config-dumping");
                if(stream == null) {
                    TerraLog.logger().send("platform.config-missing");
                } else {
                    FileUtils.copyInputStreamToFile(stream, configFile);
                }
            }
        } catch(IOException e) {
            // The sentence is styled; the trace stays on SLF4J, where a trace belongs.
            TerraLog.logger().send("platform.config-missing");
            logger.error("Error loading config.yml resource from jar", e);
        }

        config.load(this); // load config.yml


        dumpResources(config.getIgnoredResources());

        if(config.isDebugProfiler()) { // if debug.profiler is enabled, start profiling
            profiler.start();
        }

        InternalAddon internalAddon = loadAddons();

        eventManager.getHandler(FunctionalEventHandler.class)
            .register(internalAddon, PlatformInitializationEvent.class)
            .then(event -> loadConfigPacks())
            .global();

        eventManager.getHandler(FunctionalEventHandler.class)
            .register(internalAddon, PlatformInitializationEvent.class)
            .then(event -> loadMetaConfigPacks())
            .global();


        TerraLog.logger().send("platform.ready");

        return internalAddon;
    }

    protected boolean loadConfigPacks() {
        TerraLog.logger().send("platform.packs-loading");
        ConfigRegistry configRegistry = getRawConfigRegistry();
        configRegistry.clear();
        try {
            configRegistry.loadAll(this);
        } catch(IOException e) {
            TerraLog.logger().send("platform.packs-failed");
            logger.error("Failed to load config packs", e);
            return false;
        } catch(PackLoadFailuresException e) {
            // The packs that did load are registered: loadAll registers each one and throws at the end.
            // Reporting only the failure would hide the ones that are usable.
            TerraLog.logger().send("platform.packs-partial", Map.of(
                "count", Integer.toString(configRegistry.entries().size()),
                "failed", Integer.toString(e.getExceptions().size())));
            e.getExceptions().forEach(ex -> logger.error("Failed to load config pack", ex));
            return false;
        }
        TerraLog.logger().send("platform.packs-loaded", "count", Integer.toString(configRegistry.entries().size()));
        return true;
    }

    protected boolean loadMetaConfigPacks() {
        TerraLog.logger().send("platform.metapacks-loading");
        MetaConfigRegistry metaConfigRegistry = getRawMetaConfigRegistry();
        metaConfigRegistry.clear();
        try {
            metaConfigRegistry.loadAll(this, configRegistry);
        } catch(IOException e) {
            TerraLog.logger().send("platform.metapacks-failed");
            logger.error("Failed to load meta config packs", e);
            return false;
        } catch(PackLoadFailuresException e) {
            TerraLog.logger().send("platform.metapacks-partial", Map.of(
                "count", Integer.toString(metaConfigRegistry.entries().size()),
                "failed", Integer.toString(e.getExceptions().size())));
            e.getExceptions().forEach(ex -> logger.error("Failed to meta load config pack", ex));
            return false;
        }
        TerraLog.logger().send("platform.metapacks-loaded", "count", Integer.toString(metaConfigRegistry.entries().size()));
        return true;
    }

    protected InternalAddon loadAddons() {
        List<BaseAddon> addonList = new ArrayList<>();

        InternalAddon internalAddon = new InternalAddon();

        addonList.add(internalAddon);

        platformAddon().forEach(addonList::add);

        BootstrapAddonLoader bootstrapAddonLoader = new BootstrapAddonLoader();

        Path addonsFolder = getDataFolder().toPath().resolve("addons");

        Injector<Platform> platformInjector = new InjectorImpl<>(this);
        platformInjector.addExplicitTarget(Platform.class);

        BootstrapAddonClassLoader bootstrapAddonClassLoader = new BootstrapAddonClassLoader(new URL[]{ }, getClass().getClassLoader());

        bootstrapAddonLoader.loadAddons(addonsFolder, bootstrapAddonClassLoader)
            .forEach(bootstrapAddon -> {
                platformInjector.inject(bootstrapAddon);

                bootstrapAddon.loadAddons(addonsFolder, bootstrapAddonClassLoader)
                    .forEach(addonList::add);
            });

        addonList.sort(Comparator.comparing(StringIdentifiable::getID));
        warnOnDuplicateAddons(addonList);
        TerraLog.logger().send("platform.addons-loaded", "count", Integer.toString(addonList.size()));
        // The addon-and-version list stays on SLF4J. It is a block, not a prefixed line, and it is read
        // out of the log file when a pack names a version range that nothing satisfies.
        if(logger.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder();
            builder.append("Loading ")
                .append(addonList.size())
                .append(" Terra addons:");

            for(BaseAddon addon : addonList) {
                builder.append("\n        ")
                    .append("- ")
                    .append(addon.getID())
                    .append("@")
                    .append(addon.getVersion().getFormatted());
            }

            logger.info(builder.toString());
        }

        DependencySorter sorter = new DependencySorter();
        addonList.forEach(sorter::add);
        sorter.sort().forEach(addon -> {
            platformInjector.inject(addon);
            addon.initialize();
            if(!(addon instanceof EphemeralAddon)) { // ephemeral addons exist only for version checking
                addonRegistry.register(addon.key(addon.getID()), addon);
            }
        });

        return internalAddon;
    }

    /**
     * Names every addon id that was loaded more than once.
     * <p>
     * The addons directory is dumped from the jar and pruned by file name, so an addon jar left behind
     * under a name the current jar no longer produces is loaded a second time rather than replaced. That
     * costs memory and registers the same loaders twice, and nothing else reports it: the id, not the
     * file, is what the rest of the tree keys on. Deleting the stale file is the fix.
     */
    private static void warnOnDuplicateAddons(List<BaseAddon> addonList) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = addonList
            .stream()
            .map(StringIdentifiable::getID)
            .filter(id -> !seen.add(id))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if(!duplicated.isEmpty()) {
            logger.warn("These addons were loaded more than once: {}. Delete the stale jars in the addons " +
                        "directory; a duplicate is a leftover file, not a second addon.", String.join(", ", duplicated));
        }
    }

    protected void dumpResources(List<String> ignoredResources) {
        try(InputStream resourcesConfig = getClass().getResourceAsStream("/resources.yml")) {
            if(resourcesConfig == null) {
                logger.info("No resources config found. Skipping resource dumping.");
                return;
            }

            Path data = getDataFolder().toPath();

            Path addonsPath = data.resolve("addons");
            Files.createDirectories(addonsPath);
            Set<Pair<Path, String>> paths = Files
                .walk(addonsPath)
                .map(path -> Pair.of(path, data.relativize(path).toString()))

                .map(Pair.mapRight(s -> {
                    if(s.contains("+")) { // remove commit hash
                        return s.substring(0, s.lastIndexOf('+'));
                    }
                    return s;
                }))

                .filter(Pair.testRight(s -> s.contains("."))) // remove patch version
                .map(Pair.mapRight(s -> s.substring(0, s.lastIndexOf('.'))))

                .filter(Pair.testRight(s -> s.contains("."))) // remove minor version
                .map(Pair.mapRight(s -> s.substring(0, s.lastIndexOf('.'))))

                .collect(Collectors.toSet());

            Set<String> pathsNoMajor = paths
                .stream()
                .filter(Pair.testRight(s -> s.contains(".")))
                .map(Pair.mapRight(s -> s.substring(0, s.lastIndexOf('.')))) // remove major version
                .map(Pair.unwrapRight())
                .collect(Collectors.toSet());


            String resourceYaml = IOUtils.toString(resourcesConfig, StandardCharsets.UTF_8);
            Map<String, List<String>> resources = new Yaml().load(resourceYaml);
            resources.forEach((dir, entries) -> entries.forEach(entry -> {
                String resourceClassPath = dir + "/" + entry;
                if(ignoredResources.contains(dir) || ignoredResources.contains(entry) || ignoredResources.contains(resourceClassPath)) {
                    logger.info("Not dumping resource {} because it is ignored.", resourceClassPath);
                } else {
                    String resourcePath = resourceClassPath.replace('/', File.separatorChar);
                    File resource = new File(getDataFolder(), resourcePath);
                    if(resource.exists())
                        return; // dont overwrite

                    try(InputStream is = getClass().getResourceAsStream("/" + resourceClassPath)) {
                        if(is == null) {
                            logger.error("Resource {} doesn't exist on the classpath!", resourcePath);
                            return;
                        }

                        paths
                            .stream()
                            .filter(Pair.testRight(resourcePath::startsWith))
                            .forEach(Pair.consumeLeft(path -> {
                                logger.info("Removing outdated resource {}, replacing with {}", path, resourcePath);
                                try {
                                    Files.delete(path);
                                } catch(IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }));

                        if(pathsNoMajor
                               .stream()
                               .anyMatch(resourcePath::startsWith) && // if any share name
                           paths
                               .stream()
                               .map(Pair.unwrapRight())
                               .noneMatch(resourcePath::startsWith)) { // but dont share major version
                            logger.warn(
                                "Addon {} has a new major version available. It will not be automatically updated; you will need to " +
                                "ensure " +
                                "compatibility and update manually.",
                                resourcePath);
                        }

                        logger.info("Dumping resource {}.", resource.getAbsolutePath());
                        resource.getParentFile().mkdirs();
                        resource.createNewFile();
                        try(OutputStream os = new FileOutputStream(resource)) {
                            IOUtils.copy(is, os);
                        }
                    } catch(IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }));
        } catch(IOException e) {
            logger.error("Error while dumping resources...", e);
        }
    }

    @Override
    public void register(TypeRegistry registry) {
        loaders.register(registry);
    }

    @Override
    public @NotNull PluginConfig getTerraConfig() {
        return config;
    }

    @Override
    public @NotNull CheckedRegistry<ConfigPack> getConfigRegistry() {
        return checkedConfigRegistry;
    }

    @Override
    public @NotNull CheckedRegistry<MetaPack> getMetaConfigRegistry() {
        return checkedMetaConfigRegistry;
    }


    @Override
    public @NotNull Registry<BaseAddon> getAddons() {
        return lockedAddonRegistry;
    }

    @Override
    public @NotNull EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public @NotNull Profiler getProfiler() {
        return profiler;
    }

    @Override
    public int getGenerationThreads() {
        return 1;
    }
}
