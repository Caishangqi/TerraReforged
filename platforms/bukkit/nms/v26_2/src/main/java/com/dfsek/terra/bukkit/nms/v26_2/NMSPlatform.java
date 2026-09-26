package com.dfsek.terra.bukkit.nms.v26_2;

import com.dfsek.tectonic.api.TypeRegistry;
import com.dfsek.tectonic.api.exception.LoadException;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.biome.Biome.TemperatureModifier;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Locale;

import com.dfsek.terra.addon.InternalAddon;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.util.reflection.TypeKey;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import com.dfsek.terra.bukkit.PlatformImpl;
import com.dfsek.terra.bukkit.TerraBukkitPlugin;
import com.dfsek.terra.bukkit.nms.v26_2.config.BiomeAdditionsSoundTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.BiomeMoodSoundTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.BiomeParticleConfigTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.EntityTypeTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.MusicSoundTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.SoundEventTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.SpawnCostConfig;
import com.dfsek.terra.bukkit.nms.v26_2.config.SpawnEntryConfig;
import com.dfsek.terra.bukkit.nms.v26_2.config.SpawnSettingsTemplate;
import com.dfsek.terra.bukkit.nms.v26_2.config.SpawnTypeConfig;
import com.dfsek.terra.bukkit.nms.v26_2.config.VillagerTypeTemplate;


public class NMSPlatform extends PlatformImpl {

    public NMSPlatform(TerraBukkitPlugin plugin) {
        super(plugin);

        Bukkit.getPluginManager().registerEvents(new NMSInjectListener(), plugin);
    }

    @Override
    public void register(TypeRegistry registry) {
        super.register(registry);
        registry.registerLoader(PlatformBiome.class, (type, o, loader, depthTracker) -> parseBiome((String) o, depthTracker))
            .registerLoader(Identifier.class, (type, o, loader, depthTracker) -> {
                Identifier identifier = Identifier.tryParse((String) o);
                if(identifier == null)
                    throw new LoadException("Invalid identifier: " + o, depthTracker);
                return identifier;
            })
            .registerLoader(Precipitation.class, (type, o, loader, depthTracker) -> Precipitation.valueOf(((String) o).toUpperCase(
                Locale.ROOT)))
            .registerLoader(GrassColorModifier.class,
                (type, o, loader, depthTracker) -> GrassColorModifier.valueOf(((String) o).toUpperCase(
                    Locale.ROOT)))
            // The temperature modifier loader was keyed on GrassColorModifier, so it overwrote the grass
            // loader and left VanillaBiomeProperties.temperatureModifier with no loader at all.
            .registerLoader(TemperatureModifier.class,
                (type, o, loader, depthTracker) -> TemperatureModifier.valueOf(((String) o).toUpperCase(
                    Locale.ROOT)))
            .registerLoader(MobCategory.class, (type, o, loader, depthTracker) -> MobCategory.valueOf((String) o))
            .registerLoader(AmbientParticle.class, BiomeParticleConfigTemplate::new)
            .registerLoader(SoundEvent.class, SoundEventTemplate::new)
            .registerLoader(AmbientMoodSettings.class, BiomeMoodSoundTemplate::new)
            .registerLoader(AmbientAdditionsSettings.class, BiomeAdditionsSoundTemplate::new)
            .registerLoader(Music.class, MusicSoundTemplate::new)
            .registerLoader(EntityType.class, EntityTypeTemplate::new)
            .registerLoader(SpawnCostConfig.class, SpawnCostConfig::new)
            .registerLoader(SpawnEntryConfig.class, SpawnEntryConfig::new)
            .registerLoader(SpawnTypeConfig.class, SpawnTypeConfig::new)
            .registerLoader(MobSpawnSettings.class, SpawnSettingsTemplate::new)
            // Keyed on the declared field type, not on VillagerType. Tectonic looks a loader up by the
            // type it sees on the field, and VanillaBiomeProperties declares ResourceKey<VillagerType>,
            // so a loader registered under VillagerType is never found and villager-type never loads.
            .registerLoader(new TypeKey<ResourceKey<VillagerType>>() {}.getType(), VillagerTypeTemplate::new);
    }

    @Override
    protected InternalAddon load() {
        InternalAddon internalAddon = super.load();

        this.getEventManager().getHandler(FunctionalEventHandler.class)
            .register(internalAddon, PlatformInitializationEvent.class)
            .priority(1)
            .then(event -> AwfulBukkitHacks.registerBiomes(this.getRawConfigRegistry()))
            .global();

        return internalAddon;
    }

    @Override
    protected Iterable<BaseAddon> platformAddon() {
        return List.of(new NMSAddon(this));
    }
}
