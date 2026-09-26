package com.dfsek.terra.bukkit.hooks;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;


/**
 * Runs Oraxen's own placement over the Oraxen blocks Terra has just generated.
 * <p>
 * Generation can only write block data. That is enough for a custom block to look and break like one,
 * because the mechanic is encoded in the block state, but not for what Oraxen writes beside the
 * block: its initial light, a farm block's marker, a storage block's container. Oraxen fills those in
 * {@code OraxenBlocks.place}, which needs a real loaded chunk on the owning region thread, and the
 * first moment that exists is when the generated chunk is loaded.
 * <p>
 * Nothing is recorded during generation and nothing is written to the save here. The id of a
 * noteblock, tripwire or chorus block is in its block state, so this reads back what it needs from the
 * world itself.
 * <p>
 * Loading this class requires Oraxen on the classpath. Reach it only behind a class presence check.
 */
public final class OraxenBlockPlacer implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(OraxenBlockPlacer.class);

    /**
     * The blocks {@code OraxenBlocks.getOraxenBlock} can read an id out of. It returns null for
     * anything else, which is why shaped blocks and furniture are not part of this delivery.
     */
    private static final Set<Material> CARRIERS = EnumSet.of(Material.NOTE_BLOCK, Material.TRIPWIRE, Material.CHORUS_PLANT);

    private final CustomBlocks customBlocks;

    private final Set<String> announced = ConcurrentHashMap.newKeySet();

    private OraxenBlockPlacer(CustomBlocks customBlocks) {
        this.customBlocks = customBlocks;
    }

    public static void register(Plugin plugin, CustomBlocks customBlocks) {
        Bukkit.getPluginManager().registerEvents(new OraxenBlockPlacer(customBlocks), plugin);
    }

    /**
     * Only chunks Terra has just generated. Two things follow from that and neither is an optimisation.
     * <p>
     * A chunk that already existed may hold Oraxen blocks a player placed, and running placement over
     * one of those resets what the mechanic stores: a storage block would come back empty. So this
     * never revisits a chunk, which also means a chunk generated and then lost to a crash before it
     * loaded keeps block data with none of the state around it. That is visible only for the mechanics
     * that have such state, and is the price of not being able to tell the two cases apart.
     * <p>
     * A world Terra did not generate may hold vanilla tripwire or chorus plants whose block data
     * collides with an Oraxen variant, and placing over those would be destruction rather than repair.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if(!event.isNewChunk() || !customBlocks.claimedAnything()) {
            return;
        }

        Chunk chunk = event.getChunk();
        if(!(chunk.getWorld().getGenerator() instanceof BukkitChunkGeneratorWrapper)) {
            return;
        }

        int placed = place(chunk);
        if(placed > 0) {
            logger.debug("Finished {} Oraxen blocks in chunk {}, {} of {}.", placed, chunk.getX(), chunk.getZ(),
                chunk.getWorld().getName());
        }
    }

    private int place(Chunk chunk) {
        World world = chunk.getWorld();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);

        // Every column of the chunk is read, so a chunk holding none of these is rejected on its
        // palettes first. Asking for the exact block data Oraxen produces is what makes that possible:
        // the vanilla default of each of these materials is not one of them.
        if(customBlocks.oraxen().all().stream().noneMatch(snapshot::contains)) {
            return 0;
        }

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        int placed = 0;

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                // The whole column, rather than down from getHighestBlockYAt. That is a motion blocking
                // height, and a tripwire does not block motion, so bounding the scan by it skips every
                // STRING block sitting on the surface — which is where a flower goes.
                for(int y = maxHeight - 1; y >= minHeight; y--) {
                    if(!CARRIERS.contains(snapshot.getBlockType(x, y, z))) {
                        continue;
                    }

                    BlockData data = snapshot.getBlockData(x, y, z);
                    Mechanic mechanic = OraxenBlocks.getOraxenBlock(data);
                    if(mechanic == null) {
                        continue;
                    }

                    String id = mechanic.getItemID();
                    // Once per block per world, at info. A line per generated chunk would be noise, but
                    // which custom blocks a world actually produces is worth saying once, and it is the
                    // only evidence that a mechanic was recognised rather than merely written.
                    if(announced.add(world.getUID() + " " + id)) {
                        logger.info("Finishing Oraxen block {} in world {}.", id, world.getName());
                    }

                    OraxenBlocks.place(id, new Location(world, (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z));
                    placed++;
                }
            }
        }

        return placed;
    }
}
