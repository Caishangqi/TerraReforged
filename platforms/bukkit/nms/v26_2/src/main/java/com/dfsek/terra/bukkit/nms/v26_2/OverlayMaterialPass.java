package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.world.chunk.generation.util.Palette;
import com.dfsek.terra.api.world.generation.overlay.OverlayMaterialSettings;
import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;


final class OverlayMaterialPass {
    private final OverlayBiomeSource biomeSource;
    private final long seed;

    OverlayMaterialPass(OverlayBiomeSource biomeSource, long seed) {
        this.biomeSource = biomeSource;
        this.seed = seed;
    }

    void apply(ChunkAccess chunk, RandomState randomState) {
        int minY = chunk.getMinY();
        for(int localX = 0; localX < 16; localX++) {
            for(int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunk.getPos().getMinBlockX() + localX;
                int worldZ = chunk.getPos().getMinBlockZ() + localZ;
                int surfaceTop = chunk.getHeight(Types.OCEAN_FLOOR_WG, localX, localZ) + 1;
                if(surfaceTop <= minY) {
                    continue;
                }
                OverlayMaterialSettings settings = biomeSource.materialsAt(worldX, surfaceTop - 1, worldZ, randomState.sampler());
                if(settings == null) {
                    continue;
                }
                applyRange(chunk, localX, localZ, worldX, worldZ, surfaceTop, settings);
            }
        }
    }

    private int applyRange(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ, int surfaceTop,
                           OverlayMaterialSettings settings) {
        int changedBlocks = 0;
        int minY = chunk.getMinY();
        int maxY = Math.min(chunk.getHeightAccessorForGeneration().getMaxY(), surfaceTop);
        for(int y = minY; y < maxY; y++) {
            if(!OverlayMaterialPredicate.inConfiguredRange(y, surfaceTop, settings)) {
                continue;
            }
            BlockPos pos = new BlockPos(worldX, y, worldZ);
            var current = chunk.getBlockState(localX, y, localZ);
            if(!OverlayMaterialPredicate.replaceable(current)) {
                continue;
            }
            boolean surface = OverlayMaterialPredicate.inSurfaceRange(y, surfaceTop, settings);
            Palette palette = surface ? settings.surface() : settings.underground();
            int layer = surface
                ? surfaceTop - 1 - y
                : (surfaceTop - settings.undergroundFromSurface() - 1) - y;
            BlockState replacement = palette.get(layer, worldX, y, worldZ, seed);
            if(replacement instanceof BukkitBlockState bukkitState) {
                chunk.setBlockState(pos, ((CraftBlockData) bukkitState.getHandle()).getState(), 0);
                changedBlocks++;
            }
        }
        return changedBlocks;
    }
}
