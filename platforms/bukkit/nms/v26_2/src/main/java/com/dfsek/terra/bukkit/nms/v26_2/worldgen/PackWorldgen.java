package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.properties.Context;
import com.dfsek.terra.api.properties.Properties;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.generation.vanilla.VanillaFeatureStep;
import com.dfsek.terra.api.world.generation.vanilla.VanillaGeneration;
import com.dfsek.terra.bukkit.nms.v26_2.RegistryFetcher;


/**
 * The world generation objects one pack declares, decoded and keyed by resource location.
 * <p>
 * Held in the pack context, built once at pack load, and read afterwards by biome registration and by the
 * chunk generator. The settings map is the only part written later: biome registration fills it before any
 * chunk is generated, and reads then come from generation threads.
 */
public final class PackWorldgen implements Properties {
    public static final PackWorldgen EMPTY = new PackWorldgen(
        new MappedRegistry<>(Registries.CONFIGURED_FEATURE, Lifecycle.stable()),
        new MappedRegistry<>(Registries.PLACED_FEATURE, Lifecycle.stable()),
        new MappedRegistry<>(Registries.CONFIGURED_CARVER, Lifecycle.stable()),
        null);

    private final Registry<ConfiguredFeature<?, ?>> configuredFeatures;
    private final Registry<PlacedFeature> placedFeatures;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final SurfaceRules.RuleSource surfaceRule;
    private final Map<Holder<Biome>, BiomeGenerationSettings> settings = new ConcurrentHashMap<>();
    private final Map<Holder<Biome>, Holder<Biome>> vanillaBase = new ConcurrentHashMap<>();

    PackWorldgen(Registry<ConfiguredFeature<?, ?>> configuredFeatures,
                 Registry<PlacedFeature> placedFeatures,
                 Registry<ConfiguredWorldCarver<?>> carvers,
                 SurfaceRules.RuleSource surfaceRule) {
        this.configuredFeatures = configuredFeatures;
        this.placedFeatures = placedFeatures;
        this.carvers = carvers;
        this.surfaceRule = surfaceRule;
    }

    public static PackWorldgen of(ConfigPack pack) {
        Context context = pack.getContext();
        return context.has(PackWorldgen.class) ? context.get(PackWorldgen.class) : EMPTY;
    }

    public ConfiguredFeature<?, ?> configuredFeature(RegistryKey key) {
        return configuredFeatures.get(PackKeys.resourceKey(Registries.CONFIGURED_FEATURE, key))
            .map(Holder::value).orElse(null);
    }

    public PlacedFeature placedFeature(RegistryKey key) {
        return placedFeatures.get(PackKeys.resourceKey(Registries.PLACED_FEATURE, key)).map(Holder::value).orElse(null);
    }

    public ConfiguredWorldCarver<?> carver(RegistryKey key) {
        return carvers.get(PackKeys.resourceKey(Registries.CONFIGURED_CARVER, key)).map(Holder::value).orElse(null);
    }

    public boolean hasSurfaceRule() {
        return surfaceRule != null;
    }

    public SurfaceRules.RuleSource surfaceRule() {
        return surfaceRule;
    }

    /**
     * Builds the generation settings a biome declares.
     * <p>
     * A key resolves against the pack registries first and the server registries second, so a biome can mix
     * pack objects with untouched vanilla ones. Every step of the vanilla decoration order gets its own list,
     * which is the shape the feature sorter expects.
     */
    public BiomeGenerationSettings settings(VanillaGeneration generation) {
        if(generation.isEmpty()) {
            return BiomeGenerationSettings.EMPTY;
        }

        BiomeGenerationSettings.PlainBuilder builder = new BiomeGenerationSettings.PlainBuilder();
        for(VanillaFeatureStep step : VanillaFeatureStep.values()) {
            GenerationStep.Decoration decoration = GenerationStep.Decoration.valueOf(step.name());
            for(RegistryKey key : generation.features(step)) {
                builder.addFeature(decoration, placedFeatureHolder(key).orElseThrow(() ->
                    new IllegalArgumentException("No placed feature \"" + key + "\" in the pack or the server registry.")));
            }
        }
        for(RegistryKey key : generation.carvers()) {
            builder.addCarver(carverHolder(key).orElseThrow(() ->
                new IllegalArgumentException("No configured carver \"" + key + "\" in the pack or the server registry.")));
        }
        return builder.build();
    }

    public BiomeGenerationSettings settingsFor(Holder<Biome> biome) {
        return settings.getOrDefault(biome, BiomeGenerationSettings.EMPTY);
    }

    public void putSettings(Holder<Biome> biome, BiomeGenerationSettings value) {
        settings.put(biome, value);
    }

    /**
     * Records the vanilla biome a registered Terra biome extends.
     * <p>
     * The surface rule is written against vanilla biome keys, so the rule has to see the vanilla biome where
     * generation otherwise sees the Terra one.
     */
    public void putVanillaBase(Holder<Biome> terra, Holder<Biome> vanilla) {
        vanillaBase.put(terra, vanilla);
    }

    public Holder<Biome> vanillaBase(Holder<Biome> biome) {
        return vanillaBase.getOrDefault(biome, biome);
    }

    private Optional<Holder.Reference<PlacedFeature>> placedFeatureHolder(RegistryKey key) {
        ResourceKey<PlacedFeature> resourceKey = PackKeys.resourceKey(Registries.PLACED_FEATURE, key);
        Optional<Holder.Reference<PlacedFeature>> local = placedFeatures.get(resourceKey);
        return local.isPresent() ? local : RegistryFetcher.placedFeatureRegistry().get(resourceKey);
    }

    private Optional<Holder.Reference<ConfiguredWorldCarver<?>>> carverHolder(RegistryKey key) {
        ResourceKey<ConfiguredWorldCarver<?>> resourceKey = PackKeys.resourceKey(Registries.CONFIGURED_CARVER, key);
        Optional<Holder.Reference<ConfiguredWorldCarver<?>>> local = carvers.get(resourceKey);
        return local.isPresent() ? local : RegistryFetcher.carverRegistry().get(resourceKey);
    }
}
