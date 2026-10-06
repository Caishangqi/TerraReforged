package com.dfsek.terra.bukkit.hooks.craftengine;

import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * The Craft-Engine block ids this server can generate, each already resolved to the
 * {@link BlockData} Craft-Engine produces for it.
 * <p>
 * This type names no Craft-Engine classes, so it is safe to load on servers without Craft-Engine installed.
 */
public final class CraftEngineBlockTable {
    private volatile Map<String, BlockData> blocks = null;

    public boolean isLoaded() {
        return blocks != null;
    }

    public Optional<BlockData> blockData(String id) {
        Map<String, BlockData> current = blocks;
        return current == null ? Optional.empty() : Optional.ofNullable(current.get(id));
    }

    public Collection<BlockData> all() {
        Map<String, BlockData> current = blocks;
        return current == null ? List.of() : current.values();
    }

    public void replace(Map<String, BlockData> resolved) {
        blocks = Map.copyOf(resolved);
    }
}
