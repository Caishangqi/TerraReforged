package com.dfsek.terra.bukkit.hooks;

import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;
import org.bukkit.Bukkit;

import com.dfsek.terra.api.block.state.BlockState;


/**
 * Block strings that name a block belonging to another plugin rather than to Minecraft.
 * <p>
 * Which plugins those are is decided here and nowhere else, so that the platform's type loading does
 * not depend on which integrations Terra happens to have. Oraxen is the only one today.
 * <p>
 * This type names no Oraxen class. It is consulted for every block string in every pack, including on
 * servers that have no Oraxen installed.
 */
public final class CustomBlocks {
    /**
     * The prefix a pack writes, matching the form Oraxen documents for Iris.
     */
    private static final String ORAXEN_PREFIX = "oraxen:";

    private final OraxenBlockTable oraxen = new OraxenBlockTable();

    private volatile boolean claimedAnything = false;

    public OraxenBlockTable oraxen() {
        return oraxen;
    }

    /**
     * Whether any pack on this server named a custom block. The work that finishes such a block after
     * generation is per chunk, so a server that does not use the feature should not pay for it, and
     * this is the cheapest thing that can say so.
     * <p>
     * It only ever becomes true. A reload that removes the last custom block from every pack leaves it
     * set, which costs a scan that finds nothing rather than being wrong.
     */
    public boolean claimedAnything() {
        return claimedAnything;
    }

    public boolean claims(String data) {
        return data.startsWith(ORAXEN_PREFIX);
    }

    /**
     * Whether the plugin is installed is the one thing about a custom block id that is knowable while
     * packs load, so it is the one thing that fails there rather than during generation. The check asks
     * the plugin manager rather than loading a class: Paper has constructed every plugin before it
     * enables any of them, so the answer is already correct this early, and a missing plugin stays a
     * missing plugin rather than becoming a classloading question.
     *
     * @throws IllegalArgumentException if {@link #claims(String)} is false for {@code data}
     */
    public BlockState parse(String data, DepthTracker depthTracker) throws LoadException {
        if(!claims(data)) {
            throw new IllegalArgumentException("Not a custom block id: " + data);
        }

        if(Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            throw new LoadException("\"%s\" is an Oraxen block, but Oraxen is not installed on this server.".formatted(data),
                depthTracker);
        }

        claimedAnything = true;
        return new OraxenBlockState(oraxen, data.substring(ORAXEN_PREFIX.length()), depthTracker.getConfigurationName(),
            depthTracker.pathDescriptor());
    }

    /**
     * A custom block id in a position that wants a block type rather than a block to place. Nothing can
     * be deferred there: a block type is read out while packs load, which is the moment the answer does
     * not exist. Without this the id would reach {@code Bukkit.createBlockData} and fail as a parse
     * error, which says nothing about why.
     */
    public LoadException notSomethingToMatchAgainst(String data, DepthTracker depthTracker) {
        return new LoadException("""
                                 "%s" can only be used where Terra places a block, not where it matches one.
                                 Terra asks the plugin that owns the block what it is at the moment it places it, \
                                 and a match is decided while packs load, before that plugin exists.""".formatted(data),
            depthTracker);
    }
}
