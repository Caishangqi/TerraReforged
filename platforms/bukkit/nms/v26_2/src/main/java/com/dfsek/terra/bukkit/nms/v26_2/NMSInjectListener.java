package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.world.generation.GenerationMode;
import com.dfsek.terra.api.world.generation.TerrainSource;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;
import com.dfsek.terra.log.TerraLog;


public class NMSInjectListener implements Listener {
    private static final Set<World> INJECTED = new HashSet<>();
    private static final ReentrantLock INJECT_LOCK = new ReentrantLock();

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if(!(event.getWorld().getGenerator() instanceof BukkitChunkGeneratorWrapper bukkitChunkGeneratorWrapper)) {
            return;
        }

        INJECT_LOCK.lock();
        try {
            if(!INJECTED.add(event.getWorld())) {
                return;
            }
            inject(event.getWorld(), bukkitChunkGeneratorWrapper.getPack());
        } finally {
            INJECT_LOCK.unlock();
        }
    }

    private static void inject(World world, ConfigPack pack) {
        TerraLog.logger().send("platform.generator-takeover-starting", "world", world.getName());
        CraftWorld craftWorld = (CraftWorld) world;
        ServerLevel serverWorld = craftWorld.getHandle();

        ChunkGenerator installedWorldGenerator = serverWorld.getChunkSource().getGenerator();
        ChunkGenerator vanilla = installedWorldGenerator;
        ChunkGenerator terrain = installedWorldGenerator;
        if(installedWorldGenerator instanceof CustomChunkGenerator customGenerator) {
            if(pack.generationMode() == GenerationMode.OVERLAY) {
                vanilla = customGenerator.getDelegate();
                terrain = vanilla;
            } else {
                // The Bukkit generator replaces noise, surface, carvers and decoration but not the terrain
                // underneath it. A pack asking for vanilla terrain reads that generator.
                terrain = customGenerator.getDelegate();
            }
        }

        if(pack.terrainSource() == TerrainSource.VANILLA && !(terrain instanceof NoiseBasedChunkGenerator)) {
            throw new IllegalStateException("World " + world.getName() + " uses terrain-source VANILLA, but the generator " +
                                            "under the Bukkit wrapper is " + terrain.getClass().getName() +
                                            " instead of a noise generator.");
        }

        ChunkGenerator installedGenerator = generator(vanilla, terrain, pack, craftWorld.getSeed());

        ChunkMap chunkMap = serverWorld.getChunkSource().chunkMap;
        WorldGenContext worldGenContext = Reflection.CHUNKMAP.getWorldGenContext(chunkMap);
        Reflection.CHUNKMAP.setWorldGenContext(chunkMap, new WorldGenContext(
            worldGenContext.level(),
            installedGenerator,
            worldGenContext.structureManager(),
            worldGenContext.lightEngine(),
            worldGenContext.mainThreadExecutor(),
            worldGenContext.unsavedListener()
        ));

        // ChunkMap.worldGenContext is final and ServerChunkCache.getGenerator() reads through it, so a
        // write that does not land leaves vanilla answering getBaseHeight and getBaseColumn while Terra
        // terrain still appears through the Bukkit generator. That is the failure worth naming out loud.
        ChunkGenerator installed = serverWorld.getChunkSource().getGenerator();
        if(installed == installedGenerator) {
            String message = pack.generationMode() == GenerationMode.OVERLAY
                ? "platform.generator-takeover-overlay"
                : "platform.generator-takeover-full";
            TerraLog.logger().send(message, Map.of("world", world.getName()));
        } else {
            String message = pack.generationMode() == GenerationMode.OVERLAY
                ? "platform.generator-takeover-overlay-failed"
                : "platform.generator-takeover-full-failed";
            TerraLog.logger().send(message, Map.of(
                "world", world.getName(),
                "generator", installed.getClass().getName()));
        }
    }

    private static ChunkGenerator generator(ChunkGenerator vanilla, ChunkGenerator terrain, ConfigPack pack, long seed) {
        if(pack.generationMode() == GenerationMode.OVERLAY) {
            return new NMSVanillaOverlayGenerator(vanilla, pack, seed);
        }
        NMSBiomeProvider provider = new NMSBiomeProvider(pack.getBiomeProvider(), seed);
        return new NMSChunkGeneratorDelegate(vanilla, terrain, pack, provider, seed);
    }
}
