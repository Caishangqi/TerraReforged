/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.overlay;


public record BiomePlacement(String id, String biome, int priority, int order, PlacementSelector selector) {
    public BiomePlacement {
        if(id == null || id.isBlank()) {
            throw new IllegalArgumentException("Placement id must be present.");
        }
        if(biome == null || biome.isBlank()) {
            throw new IllegalArgumentException("Placement biome id must be present.");
        }
        if(order < 0) {
            throw new IllegalArgumentException("Placement declaration order must not be negative.");
        }
        if(selector == null) {
            throw new IllegalArgumentException("Placement selector must be present.");
        }
    }
}
