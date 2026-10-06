package com.dfsek.terra.bukkit.hooks;

import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.bukkit.hooks.craftengine.CraftEngineBlockProvider;
import com.dfsek.terra.bukkit.hooks.craftengine.CraftEngineBlockTable;
import com.dfsek.terra.lang.Messages;


/**
 * Block strings that name a block belonging to another plugin rather than to Minecraft.
 * <p>
 * Which plugins those are is decided here via registered {@link CustomBlockProvider} instances.
 * This type names no third-party classes, keeping platform type loading independent of integrations.
 */
public final class CustomBlocks {
    private final List<CustomBlockProvider> providers = new ArrayList<>();
    private final OraxenBlockProvider oraxen = new OraxenBlockProvider();
    private final CraftEngineBlockProvider craftEngine = new CraftEngineBlockProvider();

    private volatile boolean claimedAnything = false;

    public CustomBlocks() {
        registerProvider(oraxen);
        registerProvider(craftEngine);
    }

    public synchronized void registerProvider(CustomBlockProvider provider) {
        providers.add(provider);
    }

    public List<CustomBlockProvider> providers() {
        return Collections.unmodifiableList(providers);
    }

    public OraxenBlockTable oraxen() {
        return oraxen.table();
    }

    public OraxenBlockProvider oraxenProvider() {
        return oraxen;
    }

    public CraftEngineBlockTable craftEngine() {
        return craftEngine.table();
    }

    public CraftEngineBlockProvider craftEngineProvider() {
        return craftEngine;
    }

    /**
     * Whether any pack on this server named a custom block.
     */
    public boolean claimedAnything() {
        return claimedAnything;
    }

    public boolean claims(String data) {
        for(CustomBlockProvider provider : providers) {
            if(provider.claims(data)) {
                return true;
            }
        }
        return false;
    }

    public Optional<CustomBlockProvider> getProvider(String data) {
        for(CustomBlockProvider provider : providers) {
            if(provider.claims(data)) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }

    /**
     * Parses the custom block identifier via the matching provider.
     *
     * @throws IllegalArgumentException if {@link #claims(String)} is false for {@code data}
     * @throws LoadException           if the backing plugin is not installed
     */
    public BlockState parse(String data, DepthTracker depthTracker) throws LoadException {
        CustomBlockProvider provider = getProvider(data).orElseThrow(
            () -> new IllegalArgumentException("Not a custom block id: " + data));

        if(!provider.isInstalled()) {
            String msg = Messages.get("custom-blocks.plugin-missing", Map.of(
                "data", data,
                "plugin", provider.pluginName()
            ));
            throw new LoadException(msg, nonNullTracker(depthTracker));
        }

        claimedAnything = true;
        return provider.parse(data, depthTracker);
    }

    /**
     * A custom block id in a position that wants a block type rather than a block to place.
     */
    public LoadException notSomethingToMatchAgainst(String data, DepthTracker depthTracker) {
        String msg = Messages.get("custom-blocks.not-for-matching", Map.of(
            "data", data
        ));
        return new LoadException(msg, nonNullTracker(depthTracker));
    }

    private static DepthTracker nonNullTracker(DepthTracker tracker) {
        if(tracker != null) {
            return tracker;
        }
        return new DepthTracker(List.of(), new com.dfsek.tectonic.api.config.Configuration() {
            @Override
            public Object get(String path) {
                return null;
            }

            @Override
            public boolean contains(String path) {
                return false;
            }

            @Override
            public String getName() {
                return "dynamic";
            }
        });
    }
}
