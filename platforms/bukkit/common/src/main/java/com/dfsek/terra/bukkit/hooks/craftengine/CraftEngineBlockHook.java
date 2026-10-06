package com.dfsek.terra.bukkit.hooks.craftengine;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Fills a {@link CraftEngineBlockTable} from Craft-Engine.
 * <p>
 * Loading this class requires Craft-Engine on the classpath. Reach it only behind a class presence check.
 */
public final class CraftEngineBlockHook implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(CraftEngineBlockHook.class);

    private final CraftEngineBlockTable table;

    private CraftEngineBlockHook(CraftEngineBlockTable table) {
        this.table = table;
    }

    public static void register(Plugin plugin, CraftEngineBlockTable table) {
        CraftEngineBlockHook hook = new CraftEngineBlockHook(table);
        Bukkit.getPluginManager().registerEvents(hook, plugin);
        hook.populate();
    }

    @EventHandler
    public void onReload(CraftEngineReloadEvent event) {
        populate();
    }

    private void populate() {
        try {
            Map<Key, BlockDefinition> definitions = CraftEngineBlocks.loadedBlocks();
            if(definitions == null || definitions.isEmpty()) {
                return;
            }
            Map<String, BlockData> resolved = new HashMap<>();
            for(Map.Entry<Key, BlockDefinition> entry : definitions.entrySet()) {
                String id = entry.getKey().asString();
                BlockDefinition definition = entry.getValue();
                if(definition == null || definition.defaultState() == null) {
                    continue;
                }
                BlockData blockData = CraftEngineBlocks.getBukkitBlockData(definition.defaultState());
                if(blockData != null) {
                    resolved.put(id, blockData);
                }
            }
            table.replace(resolved);
            logger.info("Terra can generate {} of Craft-Engine's custom blocks.", resolved.size());
        } catch(Exception e) {
            logger.warn("Failed to populate Craft-Engine custom blocks.", e);
        }
    }
}
