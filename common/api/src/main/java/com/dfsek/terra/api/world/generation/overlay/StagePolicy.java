/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.overlay;


public record StagePolicy(StageAction features, StageAction structures, StageAction mobs) {
    public static final StagePolicy DEFAULT = new StagePolicy(StageAction.ADD, StageAction.ADD, StageAction.VANILLA);

    public StagePolicy {
        if(features == null || structures == null || mobs == null) {
            throw new IllegalArgumentException("Stage policy actions must be present.");
        }
    }
}
