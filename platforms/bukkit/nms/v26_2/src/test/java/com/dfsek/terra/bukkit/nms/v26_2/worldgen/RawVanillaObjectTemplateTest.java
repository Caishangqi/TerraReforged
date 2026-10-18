package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import com.dfsek.tectonic.api.exception.ValueMissingException;
import com.dfsek.tectonic.api.loader.ConfigLoader;
import com.dfsek.tectonic.yaml.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RawVanillaObjectTemplateTest {
    @Test
    public void valueSurvivesLoadingUnchanged() {
        RawVanillaObjects.PlacedFeature object = load("""
            id: birch_trees_forest
            type: VANILLA_PLACED_FEATURE
            value:
              feature: eureka_overworld:birch_trees_forest
              placement:
                - type: minecraft:count
                  count: 6
                - type: minecraft:in_square
                - type: minecraft:heightmap
                  heightmap: WORLD_SURFACE
            """);

        assertEquals("birch_trees_forest", object.id());
        assertEquals("eureka_overworld:birch_trees_forest", object.value().get("feature"));

        List<?> placement = (List<?>) object.value().get("placement");
        assertEquals(3, placement.size());
        assertEquals(6, ((Map<?, ?>) placement.get(0)).get("count"));
        assertEquals("minecraft:in_square", ((Map<?, ?>) placement.get(1)).get("type"));
        assertEquals("WORLD_SURFACE", ((Map<?, ?>) placement.get(2)).get("heightmap"));
    }

    @Test
    public void numbersKeepTheWidthTheFileGaveThem() {
        RawVanillaObjects.Noise object = loadNoise("""
            id: gravel
            value:
              firstOctave: -9
              amplitudes:
                - 1.0
                - 0.5
                - 1.0E-4
            """);

        assertEquals(-9, object.value().get("firstOctave"));
        List<?> amplitudes = (List<?>) object.value().get("amplitudes");
        assertEquals(1.0D, amplitudes.get(0));
        assertEquals(0.5D, amplitudes.get(1));
        assertEquals(1.0E-4D, amplitudes.get(2));
    }

    @Test
    public void missingValueFailsLoading() {
        ValueMissingException failure = assertThrows(ValueMissingException.class, () -> load("""
            id: birch_trees_forest
            """));

        assertTrue(failure.getMessage().toLowerCase().contains("value"));
    }

    private static RawVanillaObjects.PlacedFeature load(String yaml) {
        RawVanillaObjectTemplate<RawVanillaObjects.PlacedFeature> template =
            new RawVanillaObjectTemplate<>(RawVanillaObjects.PlacedFeature::new);
        ConfigLoader loader = new ConfigLoader();
        loader.registerLoader(Object.class, (type, value, configLoader, depthTracker) -> value);
        loader.load(template, new YamlConfiguration(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "placed_feature.yml"));
        return template.build();
    }

    private static RawVanillaObjects.Noise loadNoise(String yaml) {
        RawVanillaObjectTemplate<RawVanillaObjects.Noise> template =
            new RawVanillaObjectTemplate<>(RawVanillaObjects.Noise::new);
        ConfigLoader loader = new ConfigLoader();
        loader.registerLoader(Object.class, (type, value, configLoader, depthTracker) -> value);
        loader.load(template, new YamlConfiguration(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "noise.yml"));
        return template.build();
    }
}
