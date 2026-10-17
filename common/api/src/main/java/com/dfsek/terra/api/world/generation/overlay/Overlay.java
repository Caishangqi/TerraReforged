/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.overlay;

import java.util.List;


public record Overlay(List<BiomePlacement> placements, StagePolicy stagePolicy) {
    public Overlay {
        if(placements == null || placements.isEmpty()) {
            throw new IllegalArgumentException("Overlay must contain at least one placement.");
        }
        for(BiomePlacement placement : placements) {
            if(placement == null) {
                throw new IllegalArgumentException("Overlay placements must be present.");
            }
        }
        if(stagePolicy == null) {
            throw new IllegalArgumentException("Overlay stage policy must be present.");
        }
        placements = List.copyOf(placements);
    }

    public Overlay(List<BiomePlacement> placements) {
        this(placements, StagePolicy.DEFAULT);
    }
}
