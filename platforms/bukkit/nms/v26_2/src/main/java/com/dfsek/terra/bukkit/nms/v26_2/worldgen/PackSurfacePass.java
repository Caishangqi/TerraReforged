package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.server.level.WorldGenRegion;

import java.util.HashSet;
import java.util.Set;


/**
 * Runs the pack's surface rule on vanilla noise terrain.
 * <p>
 * The call sequence mirrors {@code NoiseBasedChunkGenerator.buildSurface}: the same noise chunk, the same
 * world generation context, and the same possible biome set. What changes is the rule tree and the biome the
 * rule sees: a Terra biome is replaced by the vanilla biome it extends, because the rule conditions are
 * written against vanilla biome keys.
 */
public final class PackSurfacePass {
    private PackSurfacePass() {
    }

    public static void apply(WorldGenRegion region, StructureManager structures, RandomState randomState,
                             ChunkAccess chunk, ChunkGenerator generator, NoiseGeneratorSettings settings,
                             PackWorldgen worldgen) {
        if(!worldgen.hasSurfaceRule() || SharedConstants.debugVoidTerrain(chunk.getPos()) ||
           SharedConstants.DEBUG_DISABLE_SURFACE) {
            return;
        }

        WorldGenerationContext context = new WorldGenerationContext(generator, region, region.getMinecraftWorld());
        BiomeManager base = region.getBiomeManager();
        BiomeManager biomeManager = base.withDifferentSource((x, y, z) ->
            worldgen.vanillaBase(base.getNoiseBiomeAtQuart(x, y, z)));
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(c -> NoiseChunk.forChunk(c, randomState,
            Beardifier.forStructuresInChunk(structures, c.getPos()), settings, fluidPicker(settings), Blender.of(region)));

        randomState.surfaceSystem().buildSurface(randomState, biomeManager, settings.useLegacyRandomSource(), context,
            chunk, noiseChunk, worldgen.surfaceRule(), possibleBiomes(region, worldgen));
    }

    /**
     * The fluid picker vanilla builds for a noise generator settings object.
     * <p>
     * {@code NoiseBasedChunkGenerator} keeps its picker in a private field with no accessor. The picker is a
     * pure function of the settings, so it is rebuilt here instead of read out by reflection.
     */
    static Aquifer.FluidPicker fluidPicker(NoiseGeneratorSettings settings) {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int seaLevel = settings.seaLevel();
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(seaLevel, settings.defaultFluid());
        return (x, y, z) -> y < Math.min(-54, seaLevel) ? lava : sea;
    }

    private static Set<Holder<Biome>> possibleBiomes(WorldGenRegion region, PackWorldgen worldgen) {
        Set<Holder<Biome>> biomes = new HashSet<>();
        ChunkPos center = region.getCenter();
        for(int z = center.z() - 1; z <= center.z() + 1; z++) {
            for(int x = center.x() - 1; x <= center.x() + 1; x++) {
                region.getChunk(x, z).collectBiomesInPalette(biomes);
            }
        }

        Set<Holder<Biome>> mapped = new HashSet<>(biomes.size());
        for(Holder<Biome> biome : biomes) {
            mapped.add(worldgen.vanillaBase(biome));
        }
        return mapped;
    }
}
