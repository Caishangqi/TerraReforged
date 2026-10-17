package com.dfsek.terra.api.world.generation.overlay;

import com.dfsek.terra.api.properties.Properties;
import com.dfsek.terra.api.world.chunk.generation.util.Palette;


public record OverlayMaterialSettings(Palette surface, int surfaceDepth, Palette underground,
                                      int undergroundFromSurface, int undergroundDepth) implements Properties {
    public OverlayMaterialSettings {
        if(surface == null && underground == null) {
            throw new IllegalArgumentException("At least one overlay material palette is required");
        }
        if(surface != null && surfaceDepth < 1) {
            throw new IllegalArgumentException("Overlay surface depth must be positive");
        }
        if(underground != null && (undergroundFromSurface < 0 || undergroundDepth < 1)) {
            throw new IllegalArgumentException("Overlay underground range is invalid");
        }
    }
}
