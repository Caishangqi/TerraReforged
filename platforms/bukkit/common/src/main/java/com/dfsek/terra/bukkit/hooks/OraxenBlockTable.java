package com.dfsek.terra.bukkit.hooks;

import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * The Oraxen block ids this server can generate, each already resolved to the block data Oraxen would
 * place for it.
 * <p>
 * This type names no Oraxen class, deliberately. It is reached from config loading and from world
 * generation, both of which run on servers without Oraxen installed, and a type that mentions Oraxen
 * cannot be loaded on one.
 * <p>
 * An entry is shared by every placement of its id. Terra never mutates a {@link BlockData}: its
 * {@code BlockState.set} is unimplemented, and the write paths only read the handle.
 */
public final class OraxenBlockTable {
    private volatile Map<String, BlockData> blocks = null;

    /**
     * Whether Oraxen has told us what its blocks are yet. Distinguishing this from an unknown id is
     * what lets a failure say whether the server has no Oraxen or the pack has a typo.
     */
    public boolean isLoaded() {
        return blocks != null;
    }

    public Optional<BlockData> blockData(String id) {
        Map<String, BlockData> current = blocks;
        return current == null ? Optional.empty() : Optional.ofNullable(current.get(id));
    }

    /**
     * Every block data Oraxen can produce, for asking a chunk whether it holds any of them before
     * looking at it block by block.
     */
    public Collection<BlockData> all() {
        Map<String, BlockData> current = blocks;
        return current == null ? List.of() : current.values();
    }

    /**
     * Replaces the table with what Oraxen says now. Oraxen rebuilds its mechanic factories on reload,
     * so a table resolved against the previous generation of them is stale rather than merely old.
     */
    void replace(Map<String, BlockData> resolved) {
        blocks = Map.copyOf(resolved);
    }
}
