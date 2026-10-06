package com.dfsek.terra.bukkit.hooks.craftengine;

import org.bukkit.block.data.BlockData;

import java.util.Map;

import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;
import com.dfsek.terra.lang.Messages;


/**
 * A block Terra will place once Craft-Engine has resolved what its block state is.
 * <p>
 * This type names no Craft-Engine classes, so it remains safe to load without Craft-Engine.
 */
public final class CraftEngineBlockState extends BukkitBlockState {
    private final CraftEngineBlockTable table;
    private final String id;
    private final String configuration;
    private final String path;

    public CraftEngineBlockState(CraftEngineBlockTable table, String id, String configuration, String path) {
        this.table = table;
        this.id = id;
        this.configuration = configuration;
        this.path = path;
    }

    @Override
    public BlockData getHandle() {
        return table.blockData(id).orElseThrow(() -> new IllegalStateException(explain()));
    }

    private String explain() {
        String origin = "\"craftengine:%s\", named by %s at %s".formatted(id, configuration, path);

        if(!table.isLoaded()) {
            return Messages.get("custom-blocks.not-loaded", Map.of(
                "data", origin,
                "plugin", "CraftEngine"
            ));
        }

        return Messages.get("custom-blocks.unknown-block", Map.of(
            "data", origin,
            "plugin", "CraftEngine"
        ));
    }
}
