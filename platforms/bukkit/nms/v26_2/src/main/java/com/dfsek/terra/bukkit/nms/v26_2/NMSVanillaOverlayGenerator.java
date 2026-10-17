package com.dfsek.terra.bukkit.nms.v26_2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.dfsek.terra.api.config.ConfigPack;


public class NMSVanillaOverlayGenerator extends ChunkGenerator {
    private final ChunkGenerator vanilla;
    private final ConfigPack pack;
    private final OverlayBiomeSource overlayBiomeSource;
    private final OverlayMaterialPass materialPass;
    private final OverlayStageExecutor stages;

    public NMSVanillaOverlayGenerator(ChunkGenerator vanilla, ConfigPack pack, long seed) {
        this(vanilla, pack, new OverlayBiomeSource(vanilla.getBiomeSource(), pack, seed), seed);
    }

    private NMSVanillaOverlayGenerator(ChunkGenerator vanilla, ConfigPack pack, OverlayBiomeSource overlayBiomeSource,
                                       long seed) {
        super(overlayBiomeSource, ignored -> net.minecraft.world.level.biome.BiomeGenerationSettings.EMPTY);
        this.vanilla = vanilla;
        this.pack = pack;
        this.overlayBiomeSource = overlayBiomeSource;
        this.materialPass = new OverlayMaterialPass(overlayBiomeSource, seed);
        this.stages = new OverlayStageExecutor(vanilla, pack);
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() {
        return MapCodec.assumeMapUnsafe(ChunkGenerator.CODEC);
    }

    @Override
    public void applyCarvers(@NotNull WorldGenRegion chunkRegion, long seed, @NotNull RandomState noiseConfig, @NotNull BiomeManager world,
                             @NotNull StructureManager structureAccessor, @NotNull ChunkAccess chunk) {
        vanilla.applyCarvers(chunkRegion, seed, noiseConfig, world, structureAccessor, chunk);
    }

    @Override
    public void buildSurface(@NotNull WorldGenRegion region, @NotNull StructureManager structures, @NotNull RandomState noiseConfig,
                             @NotNull ChunkAccess chunk) {
        vanilla.buildSurface(region, structures, noiseConfig, chunk);
        materialPass.apply(chunk, noiseConfig);
    }

    @Override
    public void applyBiomeDecoration(@NotNull WorldGenLevel world, @NotNull ChunkAccess chunk,
                                     @NotNull StructureManager structureAccessor) {
        stages.decorate(world, chunk, structureAccessor, () -> addVanillaDecorations(world, chunk, structureAccessor));
        overlayBiomeSource.release(chunk);
    }

    @Override
    public void createStructures(@NotNull RegistryAccess registryAccess, @NotNull ChunkGeneratorStructureState state,
                                 @NotNull StructureManager structureManager, @NotNull ChunkAccess centerChunk,
                                 @NotNull StructureTemplateManager structureTemplateManager, @NotNull ResourceKey<Level> level) {
        stages.createStructures(registryAccess, state, structureManager, centerChunk, structureTemplateManager, level);
    }

    @Override
    public void createReferences(@NotNull WorldGenLevel level, @NotNull StructureManager structureManager,
                                 @NotNull ChunkAccess centerChunk) {
        vanilla.createReferences(level, structureManager, centerChunk);
    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion region) {
        stages.spawnMobs(region);
    }

    @Override
    public int getGenDepth() {
        return vanilla.getGenDepth();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(@NotNull Blender blender, @NotNull RandomState noiseConfig,
                                                        @NotNull StructureManager structureAccessor, @NotNull ChunkAccess chunk) {
        return vanilla.fillFromNoise(blender, noiseConfig, structureAccessor, chunk);
    }

    @Override
    public int getSeaLevel() {
        return vanilla.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return vanilla.getMinY();
    }

    @Override
    public int getBaseHeight(int x, int z, @NotNull Types heightmap, @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {
        return vanilla.getBaseHeight(x, z, heightmap, world, noiseConfig);
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {
        return vanilla.getBaseColumn(x, z, world, noiseConfig);
    }

    @Override
    public void addDebugScreenInfo(@NotNull List<String> text, @NotNull RandomState noiseConfig, @NotNull BlockPos pos) {
        vanilla.addDebugScreenInfo(text, noiseConfig, pos);
    }
}
