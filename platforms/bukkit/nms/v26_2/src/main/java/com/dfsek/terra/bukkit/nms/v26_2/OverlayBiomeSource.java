package com.dfsek.terra.bukkit.nms.v26_2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.properties.Context;
import com.dfsek.terra.api.properties.Properties;
import com.dfsek.terra.api.properties.PropertyHolder;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.OverlayMaterialSettings;
import com.dfsek.terra.bukkit.world.BukkitPlatformBiome;
import com.dfsek.terra.world.generation.overlay.PlacementMatcher;
import com.dfsek.terra.world.generation.overlay.PlacementQuery;


public class OverlayBiomeSource extends BiomeSource {
    private final BiomeSource vanilla;
    private final ConfigPack pack;
    private final long seed;
    private final Map<String, Holder<Biome>> customHolders;
    private final Map<Holder<Biome>, TerraBiome> customBiomes = new HashMap<>();
    private final Map<BiomePosition, Holder<Biome>> retainedSources = new ConcurrentHashMap<>();

    public OverlayBiomeSource(BiomeSource vanilla, ConfigPack pack, long seed) {
        this.vanilla = vanilla;
        this.pack = pack;
        this.seed = seed;
        this.customHolders = customHolders(pack);
    }

    public OverlayMaterialSettings materialsAt(int x, int y, int z, Sampler sampler) {
        Holder<Biome> holder = getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
        TerraBiome biome = customBiomes.get(holder);
        if(biome == null || !biome.getContext().has(OverlayMaterialSettings.class)) {
            return null;
        }
        return biome.getContext().get(OverlayMaterialSettings.class);
    }

    public Holder<Biome> sourceAt(int x, int y, int z, Sampler sampler) {
        BiomePosition position = new BiomePosition(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
        return retainedSources.computeIfAbsent(position, ignored ->
            vanilla.getNoiseBiome(position.x(), position.y(), position.z(), sampler));
    }

    public void release(ChunkAccess chunk) {
        int minX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int minY = QuartPos.fromBlock(chunk.getMinY());
        int minZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        int quartHeight = chunk.getHeight() >> 2;
        for(int x = minX; x < minX + 4; x++) {
            for(int y = minY; y < minY + quartHeight; y++) {
                for(int z = minZ; z < minZ + 4; z++) {
                    retainedSources.remove(new BiomePosition(x, y, z));
                }
            }
        }
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(vanilla.possibleBiomes().stream(), customHolders.values().stream()).distinct();
    }

    @Override
    protected @NotNull MapCodec<? extends BiomeSource> codec() {
        return MapCodec.assumeMapUnsafe(BiomeSource.CODEC);
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(int x, int y, int z, @NotNull Sampler sampler) {
        int blockX = x << 2;
        int blockY = y << 2;
        int blockZ = z << 2;
        Holder<Biome> source = sourceAt(blockX, blockY, blockZ, sampler);
        String sourceId = source.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        PlacementQuery query = new PlacementQuery(seed, blockX, blockY, blockZ, sourceId);
        Optional<BiomePlacement> match = PlacementMatcher.firstMatch(pack.overlay().placements(), query, this::sample);
        if(match.isEmpty()) {
            return source;
        }
        Holder<Biome> custom = customHolders.get(match.get().biome());
        return custom == null ? source : custom;
    }

    private double sample(String samplerId, long sampleSeed, int x, int y, int z) {
        Object sampler = sampler(samplerId);
        if(sampler == null) {
            return Double.NEGATIVE_INFINITY;
        }
        try {
            Object value = sampler.getClass().getMethod("getSample", long.class, double.class, double.class)
                .invoke(sampler, sampleSeed, (double) x, (double) z);
            return value instanceof Number number ? number.doubleValue() : Double.NEGATIVE_INFINITY;
        } catch(ReflectiveOperationException exception) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private Object sampler(String samplerId) {
        for(Properties properties : pack.getContext().values()) {
            if(!properties.getClass().getName().equals("com.dfsek.terra.addons.noise.NoiseConfigPackTemplate")) {
                continue;
            }
            try {
                Object samplers = properties.getClass().getMethod("getSamplers").invoke(properties);
                if(!(samplers instanceof Map<?, ?> map)) {
                    return null;
                }
                Object configured = map.get(samplerId);
                if(configured == null) {
                    return null;
                }
                return configured.getClass().getMethod("getSampler").invoke(configured);
            } catch(ReflectiveOperationException exception) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Holder<Biome>> customHolders(ConfigPack pack) {
        Map<String, Holder<Biome>> holders = new HashMap<>();
        BiomeProvider provider = pack.getBiomeProvider();
        if(provider == null) {
            return holders;
        }
        provider.stream().forEach(biome -> {
            ResourceKey<Biome> key = ((BukkitPlatformBiome) biome.getPlatformBiome()).getContext().get(NMSBiomeInfo.class).biomeKey();
            Holder<Biome> holder = RegistryFetcher.biomeRegistry().getOrThrow(key);
            holders.put(biome.getID(), holder);
            customBiomes.put(holder, new TerraBiome(biome));
        });
        return holders;
    }

    private record BiomePosition(int x, int y, int z) {
    }

    private record TerraBiome(PropertyHolder delegate) {
        private Context getContext() {
            return delegate.getContext();
        }
    }
}
