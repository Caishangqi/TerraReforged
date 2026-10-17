package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.world.generation.overlay.StageAction;


public final class OverlayStageExecutor {
    private final ChunkGenerator vanilla;
    private final ConfigPack pack;

    public OverlayStageExecutor(ChunkGenerator vanilla, ConfigPack pack) {
        this.vanilla = vanilla;
        this.pack = pack;
    }

    public void decorate(WorldGenLevel world, ChunkAccess chunk, StructureManager structureManager, Runnable placeStructurePieces) {
        StageAction action = pack.overlay().stagePolicy().features();
        boolean includeVanillaFeatures = action == StageAction.ADD || action == StageAction.VANILLA;
        if(includeVanillaFeatures) {
            vanilla.applyBiomeDecoration(world, chunk, structureManager);
        } else {
            placeStructurePieces.run();
            vanilla.applyBiomeDecoration(world, chunk, structureManager, false);
        }
    }

    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState state, StructureManager structureManager,
                                 ChunkAccess centerChunk, StructureTemplateManager structureTemplateManager,
                                 ResourceKey<Level> level) {
        StageAction action = pack.overlay().stagePolicy().structures();
        if(action == StageAction.REPLACE || action == StageAction.DISABLE) {
            return;
        }
        vanilla.createStructures(registryAccess, state, structureManager, centerChunk, structureTemplateManager, level);
    }

    public void spawnMobs(WorldGenRegion region) {
        if(pack.overlay().stagePolicy().mobs() == StageAction.VANILLA) {
            vanilla.spawnOriginalMobs(region);
        }
    }
}
