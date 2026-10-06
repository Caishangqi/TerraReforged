package com.dfsek.terra.bukkit.hooks;

import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;
import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.Optional;

import com.dfsek.terra.api.block.state.BlockState;


/**
 * SPI provider for custom blocks supplied by an external Bukkit plugin.
 */
public interface CustomBlockProvider {
    /**
     * The primary prefix used in config packs (e.g. "oraxen:", "craftengine:").
     */
    String prefix();

    /**
     * Whether this provider claims ownership of the given block string.
     * Default implementation checks whether {@code data} starts with {@link #prefix()}.
     */
    default boolean claims(String data) {
        return data.startsWith(prefix());
    }

    /**
     * The plugin name as reported by Bukkit (e.g. "Oraxen", "CraftEngine").
     */
    String pluginName();

    /**
     * Whether the backing plugin is installed on this server.
     */
    boolean isInstalled();

    /**
     * Whether the backing plugin has loaded and exported its custom block data.
     */
    boolean isLoaded();

    /**
     * Parses the custom block identifier into a deferred {@link BlockState}.
     */
    BlockState parse(String data, DepthTracker depthTracker) throws LoadException;

    /**
     * Resolves the custom block id to Bukkit {@link BlockData}, if known.
     */
    Optional<BlockData> blockData(String id);

    /**
     * Returns all block data this provider can produce (for chunk palette scanning).
     */
    Collection<BlockData> allBlockData();
}
