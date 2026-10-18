/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.biome;

import com.dfsek.tectonic.api.config.template.ConfigTemplate;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.exception.LoadException;
import com.dfsek.tectonic.api.loader.ConfigLoader;
import com.dfsek.tectonic.yaml.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.generation.vanilla.VanillaFeatureStep;
import com.dfsek.terra.api.world.generation.vanilla.VanillaGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class VanillaGenerationConfigTest {
    @Test
    public void omittedSectionIsEmpty() {
        assertTrue(new VanillaGenerationTemplate().get().isEmpty());
    }

    @Test
    public void everyStepIsPresentEvenWhenNotDeclared() {
        VanillaGeneration generation = load("""
            vanilla-generation:
              features:
                vegetal_decoration:
                  - eureka_overworld:birch_trees_forest
              carvers:
                - minecraft:cave
            """);

        for(VanillaFeatureStep step : VanillaFeatureStep.values()) {
            assertEquals(step == VanillaFeatureStep.VEGETAL_DECORATION, !generation.features(step).isEmpty(), step.name());
        }
        assertEquals(List.of(RegistryKey.parse("eureka_overworld:birch_trees_forest")),
            generation.features(VanillaFeatureStep.VEGETAL_DECORATION));
        assertEquals(List.of(RegistryKey.parse("minecraft:cave")), generation.carvers());
        assertFalse(generation.isEmpty());
    }

    @Test
    public void unknownStepFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            vanilla-generation:
              features:
                sideways:
                  - minecraft:cave
            """));

        assertTrue(failure.getMessage().contains("sideways"));
    }

    @Test
    public void malformedKeyFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            vanilla-generation:
              carvers:
                - minecraft:cave/extra
            """));

        assertTrue(failure.getMessage().contains("minecraft:cave/extra"));
        assertTrue(failure.getMessage().contains("carvers"));
    }

    @Test
    public void duplicateKeyFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            vanilla-generation:
              carvers:
                - minecraft:cave
                - minecraft:cave
            """));

        assertTrue(failure.getMessage().contains("duplicate"));
    }

    private static VanillaGeneration load(String yaml) {
        ConfigLoader loader = new ConfigLoader();
        loader.registerLoader(VanillaGeneration.class, VanillaGenerationTemplate::new);
        Holder holder = new Holder();
        loader.load(holder, new YamlConfiguration(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "biome.yml"));
        return holder.generation;
    }

    private static final class Holder implements ConfigTemplate {
        @Value("vanilla-generation")
        private VanillaGeneration generation;
    }
}
