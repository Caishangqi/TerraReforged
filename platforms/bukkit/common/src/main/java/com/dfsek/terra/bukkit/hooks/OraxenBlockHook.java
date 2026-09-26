package com.dfsek.terra.bukkit.hooks;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Fills an {@link OraxenBlockTable} from Oraxen.
 * <p>
 * Loading this class requires Oraxen on the classpath. Reach it only behind a class presence check,
 * as {@code CommonListener} does for Multiverse.
 * <p>
 * Every id is resolved in one pass here rather than on demand during generation, for two reasons that
 * are both load bearing. Oraxen builds a noteblock's data through {@code Bukkit.createBlockData},
 * which Terra already treats as not thread safe — see {@code BukkitWorldHandle.createBlockState} —
 * and resolving on the event thread keeps that call off the generation threads entirely. And
 * {@link OraxenItemsLoadedEvent} is the only signal that says when an answer is valid: Oraxen fires it
 * once its items are parsed, and again on {@code /oraxen reload}, which rebuilds the mechanic
 * factories every previous answer came from.
 */
public final class OraxenBlockHook implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(OraxenBlockHook.class);

    private final OraxenBlockTable table;

    private OraxenBlockHook(OraxenBlockTable table) {
        this.table = table;
    }

    public static void register(Plugin plugin, OraxenBlockTable table) {
        Bukkit.getPluginManager().registerEvents(new OraxenBlockHook(table), plugin);
    }

    @EventHandler
    public void onItemsLoaded(OraxenItemsLoadedEvent event) {
        Map<String, BlockData> resolved = new HashMap<>();
        for(String id : generatableIds()) {
            BlockData data = OraxenBlocks.getOraxenBlockData(id);
            if(data == null) {
                logger.warn("Oraxen lists \"{}\" as a custom block but has no block data for it. Terra cannot generate it.", id);
                continue;
            }
            resolved.put(id, data);
        }
        table.replace(resolved);
        logger.info("Terra can generate {} of Oraxen's custom blocks.", resolved.size());
    }

    /**
     * The mechanics a generated block still identifies itself as. {@code OraxenBlocks.getOraxenBlock}
     * reads an id back out of a noteblock, tripwire or chorus block's data, which is what lets Terra
     * place one without recording where it went. A shaped block is absent because that lookup returns
     * null for it, and furniture because it is not a block at all.
     */
    private static Set<String> generatableIds() {
        Set<String> ids = new HashSet<>(OraxenBlocks.getNoteBlockIDs());
        ids.addAll(OraxenBlocks.getStringBlockIDs());
        ids.addAll(OraxenBlocks.getChorusBlockIDs());
        return ids;
    }
}
