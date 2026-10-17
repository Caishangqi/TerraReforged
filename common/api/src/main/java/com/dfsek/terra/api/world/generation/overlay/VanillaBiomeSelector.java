/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.overlay;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public record VanillaBiomeSelector(Set<String> biomes) implements PlacementSelector {
    public VanillaBiomeSelector {
        if(biomes == null) {
            throw new IllegalArgumentException("Vanilla biome selector biomes must be present.");
        }
        for(String biome : biomes) {
            if(biome == null || biome.isBlank()) {
                throw new IllegalArgumentException("Vanilla biome selector ids must be present.");
            }
        }
        biomes = Collections.unmodifiableSet(new LinkedHashSet<>(biomes));
    }
}
