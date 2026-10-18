package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;


public class RegistryFetcher {
    private static <T> Registry<T> getRegistry(ResourceKey<Registry<T>> key) {
        return MinecraftServer.getServer()
            .registryAccess()
            .lookupOrThrow(key);
    }

    public static Registry<Biome> biomeRegistry() {
        return getRegistry(Registries.BIOME);
    }

    public static Registry<SoundEvent> soundEventRegistry() {
        return getRegistry(Registries.SOUND_EVENT);
    }

    public static Registry<NormalNoise.NoiseParameters> noiseRegistry() {
        return getRegistry(Registries.NOISE);
    }

    public static Registry<PlacedFeature> placedFeatureRegistry() {
        return getRegistry(Registries.PLACED_FEATURE);
    }

    public static Registry<ConfiguredWorldCarver<?>> carverRegistry() {
        return getRegistry(Registries.CONFIGURED_CARVER);
    }
}
