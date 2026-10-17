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

package com.dfsek.terra.config.pack;

import ca.solostudios.strata.version.Version;
import com.dfsek.tectonic.api.exception.LoadException;
import com.dfsek.tectonic.api.loader.ConfigLoader;
import com.dfsek.tectonic.yaml.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.dfsek.terra.api.world.generation.GenerationMode;
import com.dfsek.terra.config.loaders.VersionLoader;
import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.Overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class OverlayConfigTest {
    private static final Set<String> BIOMES = Set.of("TERRA_PLAINS");
    private static final Set<String> SAMPLERS = Set.of("overlay_noise");

    @Test
    public void omittedModeLoadsAsFull() {
        ConfigPackTemplate template = load("""
            id: TEST
            version: 1.0.0
            """);

        assertEquals(GenerationMode.FULL, template.getGenerationMode());
        assertNull(template.getOverlay());
    }

    @Test
    public void unknownModeFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            id: TEST
            version: 1.0.0
            generation-mode: SIDEWAYS
            """));

        assertTrue(failure.getMessage().contains("generation-mode"));
    }

    @Test
    public void fullPackWithoutGeneratorFailsValidation() {
        ConfigPackTemplate template = load("""
            id: TEST
            version: 1.0.0
            generation-mode: FULL
            """);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> OverlayValidator.validate(template.getGenerationMode(), template.getOverlay(), BIOMES, SAMPLERS, false));

        assertTrue(failure.getMessage().contains("generator"));
    }

    @Test
    public void emptyPlacementsFailLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            id: TEST
            version: 1.0.0
            generation-mode: OVERLAY
            overlay:
              biome-placements: []
            """));

        assertTrue(failure.getMessage().contains("biome-placements") || failure.getMessage().contains("placement"));
    }

    @Test
    public void unknownBiomeFailsWithPlacementId() {
        Overlay overlay = loadOverlay("""
            id: edge
            biome: MISSING
            priority: 1
            selector:
              type: VANILLA_BIOME
              biomes:
                - minecraft:plains
            """);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> OverlayValidator.validate(GenerationMode.OVERLAY, overlay, BIOMES, SAMPLERS, false));

        assertTrue(failure.getMessage().contains("edge"));
        assertTrue(failure.getMessage().contains("biome"));
        assertTrue(failure.getMessage().contains("MISSING"));
    }

    @Test
    public void unknownSelectorFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> loadOverlay("""
            id: edge
            biome: TERRA_PLAINS
            priority: 1
            selector:
              type: GRID
            """));

        assertTrue(failure.getMessage().contains("GRID"));
    }

    @Test
    public void emptyVanillaBiomeListFailsWithPlacementId() {
        Overlay overlay = loadOverlay("""
            id: edge
            biome: TERRA_PLAINS
            priority: 1
            selector:
              type: VANILLA_BIOME
              biomes: []
            """);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> OverlayValidator.validate(GenerationMode.OVERLAY, overlay, BIOMES, SAMPLERS, false));

        assertTrue(failure.getMessage().contains("edge"));
        assertTrue(failure.getMessage().contains("selector.biomes"));
    }

    @Test
    public void missingSamplerFailsWithPlacementId() {
        Overlay overlay = loadOverlay("""
            id: edge
            biome: TERRA_PLAINS
            priority: 1
            selector:
              type: NOISE
              sampler: absent
              threshold: 0.5
            """);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> OverlayValidator.validate(GenerationMode.OVERLAY, overlay, BIOMES, SAMPLERS, false));

        assertTrue(failure.getMessage().contains("edge"));
        assertTrue(failure.getMessage().contains("sampler"));
        assertTrue(failure.getMessage().contains("absent"));
    }

    @Test
    public void mobsAddAndDisableFail() {
        for(String action : List.of("ADD", "DISABLE")) {
            String policy = "stage-policy:\n  mobs: " + action;
            Overlay overlay = loadOverlay("""
                id: edge
                biome: TERRA_PLAINS
                priority: 1
                selector:
                  type: NOISE
                  sampler: overlay_noise
                  threshold: 0.5
                """, policy);
            assertEquals(action, overlay.stagePolicy().mobs().name(), policy);

            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> OverlayValidator.validate(GenerationMode.OVERLAY, overlay, BIOMES, SAMPLERS, false),
                action);

            assertTrue(failure.getMessage().contains("mobs"));
            assertTrue(failure.getMessage().contains(action));
        }
    }

    @Test
    public void acceptedPlacementsSortByPriorityThenDeclarationOrder() {
        Overlay overlay = loadOverlay("""
            id: later
            biome: TERRA_PLAINS
            priority: 5
            selector:
              type: NOISE
              sampler: overlay_noise
              threshold: 0.2
            """, """
            id: higher
            biome: TERRA_PLAINS
            priority: 9
            selector:
              type: VANILLA_BIOME
              biomes:
                - minecraft:plains
            """, """
            id: earlier
            biome: TERRA_PLAINS
            priority: 5
            selector:
              type: NOISE
              sampler: overlay_noise
              threshold: 0.8
            """);

        Overlay validated = OverlayValidator.validate(GenerationMode.OVERLAY, overlay, BIOMES, SAMPLERS, false);

        assertEquals(List.of("higher", "later", "earlier"),
            validated.placements().stream().map(BiomePlacement::id).toList());
        assertEquals(List.of(1, 0, 2), validated.placements().stream().map(BiomePlacement::order).toList());
    }

    private static Overlay loadOverlay(String... placements) {
        StringBuilder yaml = new StringBuilder();
        for(String placement : placements) {
            yaml.append(listItem(placement));
        }
        return loadOverlay(yaml.toString(), "");
    }

    private static String listItem(String placement) {
        StringBuilder yaml = new StringBuilder();
        boolean first = true;
        for(String line : placement.stripIndent().lines().toList()) {
            if(first) {
                yaml.append("- ").append(line).append('\n');
                first = false;
            } else {
                yaml.append("  ").append(line).append('\n');
            }
        }
        return yaml.toString();
    }

    private static Overlay loadOverlay(String placements, String policy) {
        ConfigPackTemplate template = load("""
            id: TEST
            version: 1.0.0
            generation-mode: OVERLAY
            overlay:
              biome-placements:
            %s%s
            """.formatted(indent(placements), policy.isBlank() ? "" : "\n" + policy.stripIndent().indent(2)));
        return template.getOverlay();
    }

    private static ConfigPackTemplate load(String yaml) {
        ConfigLoader loader = new ConfigLoader();
        loader.registerLoader(Version.class, new VersionLoader());
        AtomicInteger order = new AtomicInteger();
        loader.registerLoader(com.dfsek.terra.api.world.generation.overlay.BiomePlacement.class, () -> {
            BiomePlacementTemplate placementTemplate = new BiomePlacementTemplate();
            placementTemplate.setOrder(order.getAndIncrement());
            return placementTemplate;
        });
        loader.registerLoader(Overlay.class, OverlayTemplate::new);
        ConfigPackTemplate template = new ConfigPackTemplate();
        loader.load(template, new YamlConfiguration(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "pack.yml"));
        return template;
    }

    private static String indent(String yaml) {
        return yaml.stripIndent().lines().map(line -> "    " + line).reduce("", (left, right) -> left + right + "\n");
    }
}
