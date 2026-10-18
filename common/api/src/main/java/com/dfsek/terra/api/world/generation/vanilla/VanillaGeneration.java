/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.generation.vanilla;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dfsek.terra.api.registry.key.RegistryKey;


/**
 * Vanilla features and carvers declared by a Terra biome.
 * <p>
 * The vanilla decoration loop visits the steps in {@link VanillaFeatureStep} declaration order, and the
 * generation settings builder wants a list for every step. The constructor therefore normalises the map
 * to an {@link EnumMap} holding an entry for all eleven steps, empty where the biome declares nothing.
 */
public record VanillaGeneration(Map<VanillaFeatureStep, List<RegistryKey>> features, List<RegistryKey> carvers) {
    public static final VanillaGeneration EMPTY = new VanillaGeneration(Map.of(), List.of());

    public VanillaGeneration {
        if(features == null) {
            throw new IllegalArgumentException("Vanilla generation features must be present.");
        }
        if(carvers == null) {
            throw new IllegalArgumentException("Vanilla generation carvers must be present.");
        }

        EnumMap<VanillaFeatureStep, List<RegistryKey>> normalised = new EnumMap<>(VanillaFeatureStep.class);
        for(VanillaFeatureStep step : VanillaFeatureStep.values()) {
            List<RegistryKey> declared = features.get(step);
            normalised.put(step, declared == null ? List.of() : keys(declared, "features for step " + step.getSerializedName()));
        }
        features = Collections.unmodifiableMap(normalised);
        carvers = keys(carvers, "carvers");
    }

    private static List<RegistryKey> keys(List<RegistryKey> keys, String what) {
        if(keys == null) {
            throw new IllegalArgumentException("Vanilla generation " + what + " must be present.");
        }
        for(RegistryKey key : keys) {
            if(key == null) {
                throw new IllegalArgumentException("Vanilla generation " + what + " contains a null key.");
            }
        }
        List<RegistryKey> copy = List.copyOf(keys);
        if(copy.size() != copy.stream().distinct().count()) {
            throw new IllegalArgumentException("Vanilla generation " + what + " contains a duplicate key.");
        }
        return copy;
    }

    public List<RegistryKey> features(VanillaFeatureStep step) {
        return features.get(step);
    }

    public boolean isEmpty() {
        return carvers.isEmpty() && features.values().stream().allMatch(List::isEmpty);
    }
}
