package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import java.util.Map;
import java.util.function.BiFunction;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.config.ConfigFactory;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.ConfigType;
import com.dfsek.terra.api.util.reflection.TypeKey;


/**
 * Config type for one kind of pack-declared vanilla object.
 * <p>
 * The five instances below share the template and factory shape and differ in their type key, which is
 * what puts them in five separate registries.
 */
public final class VanillaWorldgenConfigType<T extends RawVanillaObject>
    implements ConfigType<RawVanillaObjectTemplate<T>, T> {
    public static final TypeKey<RawVanillaObjects.ConfiguredFeature> CONFIGURED_FEATURE_KEY = new TypeKey<>() {
    };
    public static final TypeKey<RawVanillaObjects.PlacedFeature> PLACED_FEATURE_KEY = new TypeKey<>() {
    };
    public static final TypeKey<RawVanillaObjects.Carver> CARVER_KEY = new TypeKey<>() {
    };
    public static final TypeKey<RawVanillaObjects.Noise> NOISE_KEY = new TypeKey<>() {
    };
    public static final TypeKey<RawVanillaObjects.SurfaceRule> SURFACE_RULE_KEY = new TypeKey<>() {
    };

    public static final VanillaWorldgenConfigType<RawVanillaObjects.ConfiguredFeature> CONFIGURED_FEATURE =
        new VanillaWorldgenConfigType<>(CONFIGURED_FEATURE_KEY, RawVanillaObjects.ConfiguredFeature::new);
    public static final VanillaWorldgenConfigType<RawVanillaObjects.PlacedFeature> PLACED_FEATURE =
        new VanillaWorldgenConfigType<>(PLACED_FEATURE_KEY, RawVanillaObjects.PlacedFeature::new);
    public static final VanillaWorldgenConfigType<RawVanillaObjects.Carver> CARVER =
        new VanillaWorldgenConfigType<>(CARVER_KEY, RawVanillaObjects.Carver::new);
    public static final VanillaWorldgenConfigType<RawVanillaObjects.Noise> NOISE =
        new VanillaWorldgenConfigType<>(NOISE_KEY, RawVanillaObjects.Noise::new);
    public static final VanillaWorldgenConfigType<RawVanillaObjects.SurfaceRule> SURFACE_RULE =
        new VanillaWorldgenConfigType<>(SURFACE_RULE_KEY, RawVanillaObjects.SurfaceRule::new);

    private final TypeKey<T> typeKey;
    private final BiFunction<String, Map<String, Object>, T> builder;
    private final ConfigFactory<RawVanillaObjectTemplate<T>, T> factory = (config, platform) -> config.build();

    private VanillaWorldgenConfigType(TypeKey<T> typeKey, BiFunction<String, Map<String, Object>, T> builder) {
        this.typeKey = typeKey;
        this.builder = builder;
    }

    @Override
    public RawVanillaObjectTemplate<T> getTemplate(ConfigPack pack, Platform platform) {
        return new RawVanillaObjectTemplate<>(builder);
    }

    @Override
    public ConfigFactory<RawVanillaObjectTemplate<T>, T> getFactory() {
        return factory;
    }

    @Override
    public TypeKey<T> getTypeKey() {
        return typeKey;
    }
}
