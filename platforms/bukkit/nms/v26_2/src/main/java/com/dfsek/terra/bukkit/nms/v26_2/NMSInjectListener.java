package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;


public class NMSInjectListener implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NMSInjectListener.class);
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
        LOGGER.info("Preparing to take over the world: {}", world.getName());
        CraftWorld craftWorld = (CraftWorld) world;
        ServerLevel serverWorld = craftWorld.getHandle();

        ChunkGenerator vanilla = serverWorld.getChunkSource().getGenerator();
        NMSBiomeProvider provider = new NMSBiomeProvider(pack.getBiomeProvider(), craftWorld.getSeed());
        NMSChunkGeneratorDelegate delegate = new NMSChunkGeneratorDelegate(vanilla, pack, provider, craftWorld.getSeed());

        ChunkMap chunkMap = serverWorld.getChunkSource().chunkMap;
        WorldGenContext worldGenContext = Reflection.CHUNKMAP.getWorldGenContext(chunkMap);
        Reflection.CHUNKMAP.setWorldGenContext(chunkMap, new WorldGenContext(
            worldGenContext.level(),
            delegate,
            worldGenContext.structureManager(),
            worldGenContext.lightEngine(),
            worldGenContext.mainThreadExecutor(),
            worldGenContext.unsavedListener()
        ));

        // ChunkMap.worldGenContext is final and ServerChunkCache.getGenerator() reads through it, so a
        // write that does not land leaves vanilla answering getBaseHeight and getBaseColumn while Terra
        // terrain still appears through the Bukkit generator. That is the failure worth naming out loud.
        ChunkGenerator installed = serverWorld.getChunkSource().getGenerator();
        if(installed == delegate) {
            LOGGER.info("Successfully injected into world {}. Height and column queries now come from Terra.", world.getName());
        } else {
            LOGGER.error("Failed to take over world {}. The chunk source still reports {}, so height and column queries " +
                         "will fall back to vanilla.", world.getName(), installed.getClass().getName());
        }
    }
}
