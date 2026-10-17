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

package com.dfsek.terra.world.generation.overlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.NoiseSelector;
import com.dfsek.terra.api.world.generation.overlay.StageAction;
import com.dfsek.terra.api.world.generation.overlay.VanillaBiomeSelector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class OverlayBehaviorTest {
    @Test
    public void highestPriorityWins() {
        List<BiomePlacement> placements = List.of(
            vanilla("low", "TERRA_LOW", 1, 0, "minecraft:plains"),
            vanilla("high", "TERRA_HIGH", 20, 1, "minecraft:plains"));

        Optional<BiomePlacement> match = PlacementMatcher.match(placements, query("minecraft:plains"), unusedNoise());

        assertEquals("high", match.orElseThrow().id());
    }

    @Test
    public void declarationOrderBreaksPriorityTies() {
        List<BiomePlacement> placements = List.of(
            vanilla("second", "TERRA_SECOND", 5, 1, "minecraft:plains"),
            vanilla("first", "TERRA_FIRST", 5, 0, "minecraft:plains"));

        Optional<BiomePlacement> match = PlacementMatcher.match(placements, query("minecraft:plains"), unusedNoise());

        assertEquals("first", match.orElseThrow().id());
    }

    @Test
    public void noiseIgnoresSourceBiome() {
        BiomePlacement noise = new BiomePlacement("noise", "TERRA_NOISE", 1, 0, new NoiseSelector("overlay_noise", 0.5));
        PlacementQuery plains = query("minecraft:plains");
        PlacementQuery desert = query("minecraft:desert");

        assertEquals("noise", PlacementMatcher.match(List.of(noise), plains, (sampler, seed, x, y, z) -> 0.5).orElseThrow().id());
        assertEquals("noise", PlacementMatcher.match(List.of(noise), desert, (sampler, seed, x, y, z) -> 0.5).orElseThrow().id());
        assertTrue(PlacementMatcher.match(List.of(noise), desert, (sampler, seed, x, y, z) -> 0.49).isEmpty());
    }

    @Test
    public void laterRuleReadsOriginalSource() {
        List<BiomePlacement> placements = List.of(
            vanilla("desert", "TERRA_DESERT", 10, 0, "minecraft:desert"),
            vanilla("output", "TERRA_OUTPUT", 5, 1, "TERRA_DESERT"));
        PlacementQuery query = query("minecraft:plains");

        Optional<BiomePlacement> match = PlacementMatcher.match(placements, query, unusedNoise());

        assertTrue(match.isEmpty());
        assertEquals("minecraft:plains", query.sourceBiomeId());
    }

    @Test
    public void emptyMatchLeavesSourceDecisionWithCaller() {
        List<BiomePlacement> placements = List.of(vanilla("plains", "TERRA_PLAINS", 1, 0, "minecraft:plains"));
        PlacementQuery query = query("minecraft:desert");

        assertTrue(PlacementMatcher.match(placements, query, unusedNoise()).isEmpty());
        assertEquals("minecraft:desert", query.sourceBiomeId());
    }

    @Test
    public void stageActionsCallTheRequestedSources() {
        assertEquals(List.of("vanilla", "terra"), calls(StageAction.ADD, true));
        assertEquals(List.of("vanilla"), calls(StageAction.ADD, false));
        assertEquals(List.of("vanilla"), calls(StageAction.VANILLA, true));
        assertEquals(List.of("terra"), calls(StageAction.REPLACE, true));
        assertEquals(List.of(), calls(StageAction.REPLACE, false));
        assertEquals(List.of(), calls(StageAction.DISABLE, true));
    }

    private static List<String> calls(StageAction action, boolean terraConfigured) {
        List<String> calls = new ArrayList<>();
        StageComposer.compose(action, () -> calls.add("vanilla"), () -> calls.add("terra"), terraConfigured);
        return calls;
    }

    private static PlacementQuery query(String sourceBiomeId) {
        return new PlacementQuery(11L, 4, 8, 12, sourceBiomeId);
    }

    private static BiomePlacement vanilla(String id, String biome, int priority, int order, String source) {
        return new BiomePlacement(id, biome, priority, order, new VanillaBiomeSelector(Set.of(source)));
    }

    private static PlacementMatcher.Noise unusedNoise() {
        return (sampler, seed, x, y, z) -> {
            throw new AssertionError("Noise sample was not requested.");
        };
    }
}
