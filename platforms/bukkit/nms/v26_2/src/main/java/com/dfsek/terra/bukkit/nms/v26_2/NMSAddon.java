package com.dfsek.terra.bukkit.nms.v26_2;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.event.events.config.ConfigurationLoadEvent;
import com.dfsek.terra.api.event.events.config.pack.ConfigPackPostLoadEvent;
import com.dfsek.terra.api.event.events.config.pack.ConfigPackPreLoadEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.bukkit.BukkitAddon;
import com.dfsek.terra.bukkit.PlatformImpl;
import com.dfsek.terra.bukkit.nms.v26_2.config.VanillaBiomeProperties;
import com.dfsek.terra.bukkit.nms.v26_2.config.VanillaGenerationProperties;
import com.dfsek.terra.bukkit.nms.v26_2.worldgen.VanillaWorldgenConfigType;
import com.dfsek.terra.bukkit.nms.v26_2.worldgen.WorldgenDataLoader;


public class NMSAddon extends BukkitAddon {

    public NMSAddon(PlatformImpl platform) {
        super(platform);
    }

    @Override
    public void initialize() {
        super.initialize();

        terraBukkitPlugin.getEventManager()
            .getHandler(FunctionalEventHandler.class)
            .register(this, ConfigPackPreLoadEvent.class)
            .then(event -> {
                ConfigPack pack = event.getPack();
                // Vanilla object bodies are untyped maps, and Tectonic has no loader for Object. Handing the
                // raw value through keeps every declared field as the file wrote it.
                pack.applyLoader(Object.class, (type, value, loader, depthTracker) -> value);
                pack.registerConfigType(VanillaWorldgenConfigType.CONFIGURED_FEATURE, key("VANILLA_CONFIGURED_FEATURE"), 2000);
                pack.registerConfigType(VanillaWorldgenConfigType.PLACED_FEATURE, key("VANILLA_PLACED_FEATURE"), 2000);
                pack.registerConfigType(VanillaWorldgenConfigType.CARVER, key("VANILLA_CARVER"), 2000);
                pack.registerConfigType(VanillaWorldgenConfigType.NOISE, key("VANILLA_NOISE"), 2000);
                pack.registerConfigType(VanillaWorldgenConfigType.SURFACE_RULE, key("VANILLA_SURFACE_RULE"), 2000);
            })
            .global();

        terraBukkitPlugin.getEventManager()
            .getHandler(FunctionalEventHandler.class)
            .register(this, ConfigPackPostLoadEvent.class)
            .then(event -> event.getPack().getContext().put(WorldgenDataLoader.load(event.getPack())))
            .global();

        terraBukkitPlugin.getEventManager()
            .getHandler(FunctionalEventHandler.class)
            .register(this, ConfigurationLoadEvent.class)
            .then(event -> {
                if(event.is(Biome.class)) {
                    event.getLoadedObject(Biome.class).getContext().put(event.load(new VanillaBiomeProperties()));
                    event.getLoadedObject(Biome.class).getContext().put(event.load(new VanillaGenerationProperties()));
                }
            })
            .global();
    }
}
