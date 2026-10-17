package com.dfsek.terra.bukkit.nms.v26_2;

import org.junit.jupiter.api.Test;

import com.dfsek.terra.api.world.chunk.generation.util.Palette;
import com.dfsek.terra.api.world.generation.overlay.OverlayMaterialSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class OverlayMaterialPredicateTest {
    private static final int SURFACE_TOP = 64;

    @Test
    void surfaceRangeCoversDepthBelowSurfaceTop() {
        OverlayMaterialSettings settings = new OverlayMaterialSettings(palette(), 3, null, 0, 0);
        for(int depth = 1; depth <= 3; depth++) {
            assertTrue(OverlayMaterialPredicate.inSurfaceRange(SURFACE_TOP - depth, SURFACE_TOP, settings));
        }
        assertFalse(OverlayMaterialPredicate.inSurfaceRange(SURFACE_TOP - 4, SURFACE_TOP, settings));
        assertFalse(OverlayMaterialPredicate.inSurfaceRange(SURFACE_TOP, SURFACE_TOP, settings));
    }

    @Test
    void undergroundRangeStartsBelowSurfaceDepth() {
        OverlayMaterialSettings settings = new OverlayMaterialSettings(palette(), 2, palette(), 1, 3);
        int surfaceEnd = SURFACE_TOP - 2;
        assertFalse(OverlayMaterialPredicate.inUndergroundRange(surfaceEnd - 1, SURFACE_TOP, settings));
        assertTrue(OverlayMaterialPredicate.inUndergroundRange(surfaceEnd - 2, SURFACE_TOP, settings));
        assertTrue(OverlayMaterialPredicate.inUndergroundRange(surfaceEnd - 4, SURFACE_TOP, settings));
        assertFalse(OverlayMaterialPredicate.inUndergroundRange(surfaceEnd - 5, SURFACE_TOP, settings));
    }

    @Test
    void undergroundWithoutSurfacePaletteStillApplies() {
        OverlayMaterialSettings settings = new OverlayMaterialSettings(null, 0, palette(), 0, 2);
        assertTrue(OverlayMaterialPredicate.inUndergroundRange(SURFACE_TOP - 1, SURFACE_TOP, settings));
        assertFalse(OverlayMaterialPredicate.inUndergroundRange(SURFACE_TOP - 3, SURFACE_TOP, settings));
    }

    @Test
    void configuredRangeIsUnionOfBothRanges() {
        OverlayMaterialSettings settings = new OverlayMaterialSettings(palette(), 2, palette(), 1, 3);
        assertFalse(OverlayMaterialPredicate.inConfiguredRange(SURFACE_TOP, SURFACE_TOP, settings));
        assertTrue(OverlayMaterialPredicate.inConfiguredRange(SURFACE_TOP - 1, SURFACE_TOP, settings));
        assertFalse(OverlayMaterialPredicate.inConfiguredRange(SURFACE_TOP - 3, SURFACE_TOP, settings));
        assertTrue(OverlayMaterialPredicate.inConfiguredRange(SURFACE_TOP - 4, SURFACE_TOP, settings));
        assertFalse(OverlayMaterialPredicate.inConfiguredRange(SURFACE_TOP - 7, SURFACE_TOP, settings));
    }

    @Test
    void settingsWithoutPalettesAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new OverlayMaterialSettings(null, 0, null, 0, 0));
    }

    @Test
    void surfaceDepthMustBePositive() {
        assertThrows(IllegalArgumentException.class,
            () -> new OverlayMaterialSettings(palette(), 0, null, 0, 0));
    }

    @Test
    void undergroundDepthMustBePositive() {
        assertThrows(IllegalArgumentException.class,
            () -> new OverlayMaterialSettings(palette(), 1, palette(), 0, 0));
    }

    @Test
    void replaceableKindClassification() {
        assertTrue(OverlayMaterialPredicate.replaceable(OverlayMaterialPredicate.BlockKind.SOLID));
        assertFalse(OverlayMaterialPredicate.replaceable(OverlayMaterialPredicate.BlockKind.AIR));
        assertFalse(OverlayMaterialPredicate.replaceable(OverlayMaterialPredicate.BlockKind.FLUID));
        assertFalse(OverlayMaterialPredicate.replaceable(OverlayMaterialPredicate.BlockKind.STRUCTURE));
        assertFalse(OverlayMaterialPredicate.replaceable(OverlayMaterialPredicate.BlockKind.BLOCK_ENTITY));
    }

    private static Palette palette() {
        return new Palette() {
            @Override
            public com.dfsek.terra.api.block.state.BlockState get(int layer, double x, double y, double z, long seed) {
                return null;
            }
        };
    }
}
