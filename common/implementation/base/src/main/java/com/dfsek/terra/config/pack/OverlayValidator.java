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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.dfsek.terra.api.world.generation.GenerationMode;
import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.NoiseSelector;
import com.dfsek.terra.api.world.generation.overlay.Overlay;
import com.dfsek.terra.api.world.generation.overlay.StageAction;
import com.dfsek.terra.api.world.generation.overlay.VanillaBiomeSelector;


public final class OverlayValidator {
    private OverlayValidator() {
    }

    public static Overlay validate(GenerationMode mode, Overlay overlay, Set<String> biomeIds, Set<String> samplerIds,
                                   boolean generatorPresent) {
        if(mode == GenerationMode.FULL && !generatorPresent) {
            throw new IllegalArgumentException("Full pack is missing generator.");
        }
        if(mode != GenerationMode.OVERLAY) {
            return overlay;
        }
        if(overlay == null) {
            throw new IllegalArgumentException("Overlay pack is missing overlay.");
        }
        StageAction mobs = overlay.stagePolicy().mobs();
        if(mobs == StageAction.ADD || mobs == StageAction.DISABLE) {
            throw new IllegalArgumentException("Overlay stage-policy.mobs " + mobs + " is not valid.");
        }

        for(BiomePlacement placement : overlay.placements()) {
            if(!biomeIds.contains(placement.biome())) {
                throw new IllegalArgumentException(
                    "Placement " + placement.id() + " is missing biome " + placement.biome() + ".");
            }
            if(placement.selector() instanceof NoiseSelector noise && !samplerIds.contains(noise.sampler())) {
                throw new IllegalArgumentException(
                    "Placement " + placement.id() + " is missing sampler " + noise.sampler() + ".");
            }
            if(placement.selector() instanceof VanillaBiomeSelector vanilla && vanilla.biomes().isEmpty()) {
                throw new IllegalArgumentException(
                    "Placement " + placement.id() + " is missing selector.biomes.");
            }
        }

        List<BiomePlacement> sorted = new ArrayList<>(overlay.placements());
        sorted.sort(Comparator.comparingInt(BiomePlacement::priority).reversed().thenComparingInt(BiomePlacement::order));
        return new Overlay(sorted, overlay.stagePolicy());
    }
}
