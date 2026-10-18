package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;


/**
 * Runs the carvers a pack declares on vanilla noise terrain.
 * <p>
 * The loop mirrors {@code NoiseBasedChunkGenerator.applyCarvers}: the same source chunk radius, the same
 * seed derivation, and the same biome manager correction. The carver lists come from the pack's biome
 * generation settings, which the delegate resolves through the same getter the feature sorter uses.
 */
public final class PackCarverPass {
    private static final int SOURCE_CHUNK_RADIUS = 8;

    private PackCarverPass() {
    }

    public static void apply(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structures, ChunkAccess chunk, ChunkGenerator generator,
                             NoiseBasedChunkGenerator vanilla, NoiseGeneratorSettings settings) {
        if(SharedConstants.DEBUG_DISABLE_CARVERS) {
            return;
        }

        BiomeManager corrected = biomeManager.withDifferentSource((x, y, z) ->
            generator.getBiomeSource().getNoiseBiome(x, y, z, randomState.sampler()));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        ChunkPos pos = chunk.getPos();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(c -> NoiseChunk.forChunk(c, randomState,
            Beardifier.forStructuresInChunk(structures, c.getPos()), settings, PackSurfacePass.fluidPicker(settings),
            Blender.of(region)));
        Aquifer aquifer = noiseChunk.aquifer();
        CarvingContext context = new CarvingContext(vanilla, region.registryAccess(), chunk.getHeightAccessorForGeneration(),
            noiseChunk, randomState, settings.surfaceRule(), region.getMinecraftWorld());
        CarvingMask mask = ((ProtoChunk) chunk).getOrCreateCarvingMask();

        for(int dx = -SOURCE_CHUNK_RADIUS; dx <= SOURCE_CHUNK_RADIUS; dx++) {
            for(int dz = -SOURCE_CHUNK_RADIUS; dz <= SOURCE_CHUNK_RADIUS; dz++) {
                ChunkPos sourcePos = new ChunkPos(pos.x() + dx, pos.z() + dz);
                ChunkAccess sourceChunk = region.getChunk(sourcePos.x(), sourcePos.z());
                BiomeGenerationSettings sourceSettings = sourceChunk.carverBiome(() ->
                    generator.getBiomeGenerationSettings(generator.getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock(sourcePos.getMinBlockX()), 0, QuartPos.fromBlock(sourcePos.getMinBlockZ()),
                        randomState.sampler())));

                int index = 0;
                for(Holder<ConfiguredWorldCarver<?>> carverHolder : sourceSettings.getCarvers()) {
                    ConfiguredWorldCarver<?> carver = carverHolder.value();
                    random.setLargeFeatureSeed(seed + index, sourcePos.x(), sourcePos.z());
                    if(carver.isStartChunk(random)) {
                        carver.carve(context, chunk, corrected::getBiome, random, aquifer, sourcePos, mask);
                    }
                    index++;
                }
            }
        }
    }
}
