/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.biome;

import com.dfsek.tectonic.api.config.template.ValidatedConfigTemplate;
import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Final;
import com.dfsek.tectonic.api.config.template.annotations.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.config.AbstractableTemplate;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.meta.Meta;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import com.dfsek.terra.api.world.chunk.generation.util.Palette;
import com.dfsek.terra.api.world.generation.overlay.OverlayMaterialSettings;
import com.dfsek.terra.api.world.generation.vanilla.VanillaGeneration;


@SuppressWarnings({ "FieldMayBeFinal", "unused" })
public class BiomeTemplate implements AbstractableTemplate, ValidatedConfigTemplate {
    private final ConfigPack pack;

    @Value("id")
    @Final
    private @Meta String id;

    @Value("extends")
    @Final
    @Default
    private List<String> extended = Collections.emptyList();

    @Value("vanilla")
    private @Meta PlatformBiome vanilla;

    @Value("color")
    @Final
    @Default
    private @Meta int color = 0;
    @Value("tags")
    @Default
    private @Meta Set<@Meta String> tags = new HashSet<>();

    @Value("materials.surface.depth")
    @Default
    private Integer surfaceDepth = null;

    @Value("materials.surface.palette")
    @Default
    private @Meta Palette surfacePalette = null;

    @Value("materials.underground.from-surface")
    @Default
    private Integer undergroundFromSurface = null;

    @Value("materials.underground.depth")
    @Default
    private Integer undergroundDepth = null;

    @Value("materials.underground.palette")
    @Default
    private @Meta Palette undergroundPalette = null;

    @Value("vanilla-generation")
    @Default
    private @Meta VanillaGeneration vanillaGeneration = VanillaGeneration.EMPTY;

    public BiomeTemplate(ConfigPack pack, Platform platform) {
        this.pack = pack;
    }

    @Override
    public boolean validate() {
        color |= 0xff000000; // Alpha adjustment
        return true;
    }

    public List<String> getExtended() {
        return extended;
    }

    public Set<String> getTags() {
        return tags;
    }

    public int getColor() {
        return color;
    }

    public ConfigPack getPack() {
        return pack;
    }

    public String getID() {
        return id;
    }

    public PlatformBiome getVanilla() {
        return vanilla;
    }

    public VanillaGeneration getVanillaGeneration() {
        return vanillaGeneration;
    }

    public OverlayMaterialSettings getMaterialSettings() {
        if(surfacePalette == null && undergroundPalette == null) {
            return null;
        }
        return new OverlayMaterialSettings(surfacePalette, surfaceDepth == null ? 0 : surfaceDepth,
            undergroundPalette, undergroundFromSurface == null ? 0 : undergroundFromSurface,
            undergroundDepth == null ? 0 : undergroundDepth);
    }
}
