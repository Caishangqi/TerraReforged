/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.biome;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dfsek.terra.api.config.meta.Meta;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.generation.vanilla.VanillaFeatureStep;
import com.dfsek.terra.api.world.generation.vanilla.VanillaGeneration;


/**
 * Loads the {@code vanilla-generation} section of a biome config.
 * <p>
 * Stage names are the vanilla serialized names. The section is optional, and a step left out of the file
 * assembles as an empty list rather than a missing entry.
 */
public class VanillaGenerationTemplate implements ObjectTemplate<VanillaGeneration> {
    @Value("features")
    @Default
    private @Meta Map<String, List<String>> features = Map.of();

    @Value("carvers")
    @Default
    private @Meta List<String> carvers = List.of();

    @Override
    public VanillaGeneration get() {
        Map<VanillaFeatureStep, List<RegistryKey>> byStep = new EnumMap<>(VanillaFeatureStep.class);
        for(Map.Entry<String, List<String>> entry : features.entrySet()) {
            VanillaFeatureStep step = VanillaFeatureStep.bySerializedName(entry.getKey()).orElseThrow(() ->
                new IllegalArgumentException("Unknown vanilla feature step \"" + entry.getKey() + "\", expected one of " +
                                             Arrays.stream(VanillaFeatureStep.values())
                                                 .map(VanillaFeatureStep::getSerializedName).toList() + "."));
            byStep.put(step, keys(entry.getValue(), "features." + entry.getKey()));
        }
        return new VanillaGeneration(byStep, keys(carvers, "carvers"));
    }

    private static List<RegistryKey> keys(List<String> declared, String field) {
        List<RegistryKey> keys = new ArrayList<>(declared.size());
        for(String value : declared) {
            try {
                keys.add(RegistryKey.parse(value));
            } catch(IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid key \"" + value + "\" in vanilla-generation." + field + ": " +
                                                   e.getMessage(), e);
            }
        }
        return keys;
    }
}
