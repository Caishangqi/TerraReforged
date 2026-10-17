/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.overlay;


public record NoiseSelector(String sampler, double threshold) implements PlacementSelector {
    public NoiseSelector {
        if(sampler == null || sampler.isBlank()) {
            throw new IllegalArgumentException("Noise selector sampler id must be present.");
        }
    }
}
