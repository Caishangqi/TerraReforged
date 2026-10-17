package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.dfsek.terra.api.world.generation.overlay.OverlayMaterialSettings;


final class OverlayMaterialPredicate {
    private OverlayMaterialPredicate() {
    }

    enum BlockKind {
        AIR,
        FLUID,
        STRUCTURE,
        BLOCK_ENTITY,
        SOLID
    }

    static boolean replaceable(BlockKind kind) {
        return kind == BlockKind.SOLID;
    }

    static boolean replaceable(BlockState state) {
        BlockKind kind;
        if(state.isAir()) {
            kind = BlockKind.AIR;
        } else if(!state.getFluidState().isEmpty()) {
            kind = BlockKind.FLUID;
        } else if(state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.JIGSAW)) {
            kind = BlockKind.STRUCTURE;
        } else if(state.hasBlockEntity()) {
            kind = BlockKind.BLOCK_ENTITY;
        } else {
            kind = BlockKind.SOLID;
        }
        return replaceable(kind);
    }

    static boolean inSurfaceRange(int y, int surfaceTop, OverlayMaterialSettings settings) {
        return settings.surface() != null
            && y < surfaceTop
            && y >= surfaceTop - settings.surfaceDepth();
    }

    static boolean inUndergroundRange(int y, int surfaceTop, OverlayMaterialSettings settings) {
        if(settings.underground() == null) {
            return false;
        }
        int surfaceDepth = settings.surface() == null ? 0 : settings.surfaceDepth();
        int end = surfaceTop - surfaceDepth - settings.undergroundFromSurface();
        return y < end && y >= end - settings.undergroundDepth();
    }

    static boolean inConfiguredRange(int y, int surfaceTop, OverlayMaterialSettings settings) {
        return inSurfaceRange(y, surfaceTop, settings) || inUndergroundRange(y, surfaceTop, settings);
    }
}
