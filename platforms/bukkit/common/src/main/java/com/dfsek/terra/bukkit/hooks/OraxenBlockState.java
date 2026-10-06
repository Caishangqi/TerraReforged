package com.dfsek.terra.bukkit.hooks;

import org.bukkit.block.data.BlockData;

import java.util.Map;

import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;
import com.dfsek.terra.lang.Messages;


/**
 * A block Terra will place once Oraxen has said what it is.
 * <p>
 * Terra resolves every block string while it loads its packs, which is two to three seconds before
 * Oraxen is enabled, so the answer cannot exist yet. This stands in until the handle is first read,
 * which happens during generation.
 * <p>
 * It extends {@link BukkitBlockState} rather than implementing {@code BlockState} beside it because
 * the write paths cast to the concrete type on their way to {@code CraftBlockData}. A sibling
 * implementation would compile and then fail at generation.
 * <p>
 * Nothing is cached here. {@link OraxenBlockTable} is replaced whenever Oraxen reloads, and a handle
 * kept from before that is an answer from mechanic factories that no longer exist.
 * <p>
 * This type names no Oraxen class, so the loader that builds it stays loadable without Oraxen.
 */
public final class OraxenBlockState extends BukkitBlockState {
    private final OraxenBlockTable table;
    private final String id;
    private final String configuration;
    private final String path;

    /**
     * @param configuration the config the id was read from, and {@code path} the key within it. Both
     *                      are captured here because the failure surfaces during generation, long
     *                      after the loader that knew where the id came from has returned.
     */
    public OraxenBlockState(OraxenBlockTable table, String id, String configuration, String path) {
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
        String origin = "\"oraxen:%s\", named by %s at %s".formatted(id, configuration, path);

        if(!table.isLoaded()) {
            return Messages.get("custom-blocks.not-loaded", Map.of(
                "data", origin,
                "plugin", "Oraxen"
            ));
        }

        return Messages.get("custom-blocks.unknown-block", Map.of(
            "data", origin,
            "plugin", "Oraxen"
        ));
    }
}
