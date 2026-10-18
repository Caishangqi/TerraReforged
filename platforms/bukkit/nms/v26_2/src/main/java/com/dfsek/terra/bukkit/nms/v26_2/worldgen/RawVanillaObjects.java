package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import java.util.Map;


/**
 * The five vanilla object kinds a pack can declare, each with its own type.
 * <p>
 * Separate types keep the config registries apart. Vanilla reuses an id across kinds, for example
 * {@code minecraft:amethyst_geode} is both a configured and a placed feature, so a shared registry would
 * reject the second declaration.
 */
public final class RawVanillaObjects {
    private RawVanillaObjects() {
    }

    public record ConfiguredFeature(String id, Map<String, Object> value) implements RawVanillaObject {
    }

    public record PlacedFeature(String id, Map<String, Object> value) implements RawVanillaObject {
    }

    public record Carver(String id, Map<String, Object> value) implements RawVanillaObject {
    }

    public record Noise(String id, Map<String, Object> value) implements RawVanillaObject {
    }

    public record SurfaceRule(String id, Map<String, Object> value) implements RawVanillaObject {
    }
}
