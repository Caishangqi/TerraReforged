/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.vanilla;

import java.util.Arrays;
import java.util.Optional;


/**
 * The vanilla decoration steps, in the order the vanilla chunk generator runs them.
 * <p>
 * Names match {@code net.minecraft.world.level.levelgen.GenerationStep.Decoration} serialized names. The
 * common layer cannot reference that enum, so the NMS adapter maps these constants by name.
 */
public enum VanillaFeatureStep {
    RAW_GENERATION("raw_generation"),
    LAKES("lakes"),
    LOCAL_MODIFICATIONS("local_modifications"),
    UNDERGROUND_STRUCTURES("underground_structures"),
    SURFACE_STRUCTURES("surface_structures"),
    STRONGHOLDS("strongholds"),
    UNDERGROUND_ORES("underground_ores"),
    UNDERGROUND_DECORATION("underground_decoration"),
    FLUID_SPRINGS("fluid_springs"),
    VEGETAL_DECORATION("vegetal_decoration"),
    TOP_LAYER_MODIFICATION("top_layer_modification");

    private final String serializedName;

    VanillaFeatureStep(String serializedName) {
        this.serializedName = serializedName;
    }

    public static Optional<VanillaFeatureStep> bySerializedName(String name) {
        return Arrays.stream(values()).filter(step -> step.serializedName.equals(name)).findFirst();
    }

    public String getSerializedName() {
        return serializedName;
    }
}
