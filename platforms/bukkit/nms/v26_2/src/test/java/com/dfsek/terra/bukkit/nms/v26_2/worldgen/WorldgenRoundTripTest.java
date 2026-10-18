package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WorldgenRoundTripTest {
    private static final Gson GSON = new Gson();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void placedFeatureResolvesPackReferencesAndRoundTrips() {
        MappedRegistry<ConfiguredFeature<?, ?>> configuredFeatures =
            new MappedRegistry<>(Registries.CONFIGURED_FEATURE, Lifecycle.stable());
        MappedRegistry<PlacedFeature> placedFeatures =
            new MappedRegistry<>(Registries.PLACED_FEATURE, Lifecycle.stable());
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries = new LinkedHashMap<>();
        registries.put(Registries.CONFIGURED_FEATURE, configuredFeatures);
        registries.put(Registries.PLACED_FEATURE, placedFeatures);

        ConfiguredFeature<?, ?> declared = new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE);
        ResourceKey<ConfiguredFeature<?, ?>> declaredKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.parse("eureka_overworld:birch_trees_forest"));
        configuredFeatures.register(declaredKey, declared, RegistrationInfo.BUILT_IN);
        configuredFeatures.freeze();

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new TestProvider(registries));
        JsonElement json = GSON.toJsonTree(Map.of(
            "feature", "eureka_overworld:birch_trees_forest",
            "placement", List.of(Map.of("type", "minecraft:count", "count", 6))));

        PlacedFeature placed = decode(PlacedFeature.DIRECT_CODEC, ops, json);

        assertTrue(placed.feature().is(declaredKey));
        assertSame(declared, placed.feature().value());
        assertEquals(1, placed.placement().size());
        assertEquals(json, encode(PlacedFeature.DIRECT_CODEC, ops, placed));
    }

    @Test
    public void configuredFeatureBodyRoundTrips() {
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries = new LinkedHashMap<>();
        registries.put(Registries.CONFIGURED_FEATURE,
            new MappedRegistry<ConfiguredFeature<?, ?>>(Registries.CONFIGURED_FEATURE, Lifecycle.stable()));
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new TestProvider(registries));
        JsonElement json = GSON.toJsonTree(Map.of("type", "minecraft:no_op", "config", Map.of()));

        ConfiguredFeature<?, ?> decoded = decode(ConfiguredFeature.DIRECT_CODEC, ops, json);

        assertSame(Feature.NO_OP, decoded.feature());
        assertEquals(json, encode(ConfiguredFeature.DIRECT_CODEC, ops, decoded));
    }

    @Test
    public void configuredFeatureResolvesAPlacedFeature() {
        MappedRegistry<ConfiguredFeature<?, ?>> configuredFeatures =
            new MappedRegistry<>(Registries.CONFIGURED_FEATURE, Lifecycle.stable());
        MappedRegistry<PlacedFeature> placedFeatures =
            new MappedRegistry<>(Registries.PLACED_FEATURE, Lifecycle.stable());
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries = new LinkedHashMap<>();
        registries.put(Registries.CONFIGURED_FEATURE, configuredFeatures);
        registries.put(Registries.PLACED_FEATURE, placedFeatures);

        ConfiguredFeature<?, ?> tree = new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE);
        ResourceKey<ConfiguredFeature<?, ?>> treeKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.parse("eureka_overworld:birch_cloud_tree_tall"));
        configuredFeatures.register(treeKey, tree, RegistrationInfo.BUILT_IN);
        configuredFeatures.freeze();

        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(Registries.PLACED_FEATURE,
            Identifier.parse("eureka_overworld:birch_cloud_tree_tall"));
        placedFeatures.register(placedKey, new PlacedFeature(Holder.direct(tree), List.of()), RegistrationInfo.BUILT_IN);
        placedFeatures.freeze();

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new TestProvider(registries));
        JsonElement json = GSON.toJsonTree(Map.of(
            "type", "minecraft:random_selector",
            "config", Map.of(
                "default", "eureka_overworld:birch_cloud_tree_tall",
                "features", List.of(Map.of("chance", 1.0D, "feature", "eureka_overworld:birch_cloud_tree_tall")))));

        ConfiguredFeature<?, ?> decoded = decode(ConfiguredFeature.DIRECT_CODEC, ops, json);

        assertEquals(json, encode(ConfiguredFeature.DIRECT_CODEC, ops, decoded));
    }

    @Test
    public void surfaceRuleRoundTrips() {
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries = new LinkedHashMap<>();
        registries.put(Registries.CONFIGURED_FEATURE,
            new MappedRegistry<ConfiguredFeature<?, ?>>(Registries.CONFIGURED_FEATURE, Lifecycle.stable()));
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new TestProvider(registries));
        JsonElement json = GSON.toJsonTree(Map.of(
            "type", "minecraft:block",
            "result_state", Map.of("Name", "minecraft:stone")));

        SurfaceRules.RuleSource rule = decode(SurfaceRules.RuleSource.CODEC, ops, json);

        assertEquals(json, encode(SurfaceRules.RuleSource.CODEC, ops, rule));
    }

    private static <T> T decode(com.mojang.serialization.Codec<T> codec, RegistryOps<JsonElement> ops, JsonElement json) {
        DataResult<T> result = codec.parse(ops, json);
        List<String> errors = new ArrayList<>();
        Optional<T> decoded = result.resultOrPartial(errors::add);
        return decoded.orElseThrow(() -> new AssertionError(String.join("; ", errors)));
    }

    private static <T> JsonElement encode(com.mojang.serialization.Codec<T> codec, RegistryOps<JsonElement> ops, T value) {
        DataResult<JsonElement> result = codec.encodeStart(ops, value);
        List<String> errors = new ArrayList<>();
        Optional<JsonElement> encoded = result.resultOrPartial(errors::add);
        return encoded.orElseThrow(() -> new AssertionError(String.join("; ", errors)));
    }

    private static final class TestProvider implements HolderLookup.Provider {
        private final Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries;

        private TestProvider(Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries) {
            this.registries = registries;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(
            ResourceKey<? extends Registry<? extends T>> key) {
            return Optional.ofNullable((Registry<T>) registries.get(key));
        }

        @Override
        public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
            return registries.keySet().stream();
        }
    }
}
