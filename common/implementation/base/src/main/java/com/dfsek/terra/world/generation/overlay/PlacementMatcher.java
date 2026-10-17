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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.NoiseSelector;
import com.dfsek.terra.api.world.generation.overlay.PlacementSelector;
import com.dfsek.terra.api.world.generation.overlay.VanillaBiomeSelector;


public final class PlacementMatcher {
    @FunctionalInterface
    public interface Noise {
        double sample(String samplerId, long seed, int x, int y, int z);
    }

    private PlacementMatcher() {
    }

    public static Optional<BiomePlacement> match(List<BiomePlacement> placements, PlacementQuery query, Noise noise) {
        if(placements == null || query == null || noise == null) {
            throw new IllegalArgumentException("Placement match inputs must be present.");
        }
        List<BiomePlacement> ordered = new ArrayList<>(placements);
        ordered.sort(Comparator.comparingInt(BiomePlacement::priority).reversed().thenComparingInt(BiomePlacement::order));
        return firstMatch(ordered, query, noise);
    }

    public static Optional<BiomePlacement> firstMatch(List<BiomePlacement> orderedPlacements, PlacementQuery query, Noise noise) {
        if(orderedPlacements == null || query == null || noise == null) {
            throw new IllegalArgumentException("Placement match inputs must be present.");
        }
        String sourceBiomeId = query.sourceBiomeId();
        for(BiomePlacement placement : orderedPlacements) {
            if(matches(placement.selector(), sourceBiomeId, query, noise)) {
                return Optional.of(placement);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(PlacementSelector selector, String sourceBiomeId, PlacementQuery query, Noise noise) {
        if(selector instanceof VanillaBiomeSelector vanilla) {
            return sourceBiomeId != null && vanilla.biomes().contains(sourceBiomeId);
        }
        if(selector instanceof NoiseSelector noiseSelector) {
            double sample = noise.sample(noiseSelector.sampler(), query.seed(), query.x(), query.y(), query.z());
            return sample >= noiseSelector.threshold();
        }
        throw new IllegalArgumentException("Unknown placement selector " + selector.getClass().getName() + ".");
    }
}
