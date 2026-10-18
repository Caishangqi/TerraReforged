package com.dfsek.terra.bukkit.nms.v26_2.config;

import com.dfsek.tectonic.api.config.template.ConfigTemplate;
import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;

import com.dfsek.terra.api.properties.Properties;
import com.dfsek.terra.api.world.generation.vanilla.VanillaGeneration;


/**
 * The {@code vanilla-generation} section of a biome config, read on the NMS side.
 * <p>
 * The biome addon parses the same section for its own validation. This copy lets biome registration reach the
 * declared keys without depending on that addon's template object.
 */
public class VanillaGenerationProperties implements ConfigTemplate, Properties {
    @Value("vanilla-generation")
    @Default
    private VanillaGeneration generation = VanillaGeneration.EMPTY;

    public VanillaGeneration get() {
        return generation;
    }
}
