/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation;


/**
 * Where a {@code FULL} config pack takes its terrain shape from.
 * <p>
 * {@link #VANILLA} leaves the noise terrain to the installed generator and lets the pack replace the
 * surface rule, the carvers, and the features. {@link #TERRA} builds the terrain from the pack's
 * palettes.
 */
public enum TerrainSource {
    TERRA,
    VANILLA
}
