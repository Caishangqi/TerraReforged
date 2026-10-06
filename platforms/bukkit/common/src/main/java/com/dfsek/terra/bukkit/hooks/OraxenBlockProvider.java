package com.dfsek.terra.bukkit.hooks;

import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.Optional;

import com.dfsek.terra.api.block.state.BlockState;


/**
 * {@link CustomBlockProvider} implementation for Oraxen custom blocks.
 * Names no Oraxen classes itself, remaining safe to load on servers without Oraxen.
 */
public final class OraxenBlockProvider implements CustomBlockProvider {
    public static final String PREFIX = "oraxen:";

    private final OraxenBlockTable table = new OraxenBlockTable();

    public OraxenBlockTable table() {
        return table;
    }

    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public String pluginName() {
        return "Oraxen";
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getServer() != null && Bukkit.getPluginManager() != null && Bukkit.getPluginManager().getPlugin("Oraxen") != null;
    }

    @Override
    public boolean isLoaded() {
        return table.isLoaded();
    }

    @Override
    public BlockState parse(String data, DepthTracker depthTracker) throws LoadException {
        String configName = depthTracker != null ? depthTracker.getConfigurationName() : "dynamic";
        String pathDesc = depthTracker != null ? depthTracker.pathDescriptor() : "script";
        return new OraxenBlockState(table, data.substring(PREFIX.length()), configName, pathDesc);
    }

    @Override
    public Optional<BlockData> blockData(String id) {
        return table.blockData(id);
    }

    @Override
    public Collection<BlockData> allBlockData() {
        return table.all();
    }
}
