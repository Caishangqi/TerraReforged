package com.dfsek.terra.bukkit.hooks.craftengine;

import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.Optional;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.bukkit.hooks.CustomBlockProvider;


/**
 * {@link CustomBlockProvider} implementation for Craft-Engine custom blocks.
 * Names no Craft-Engine classes itself, so it remains safe to load without Craft-Engine.
 */
public final class CraftEngineBlockProvider implements CustomBlockProvider {
    public static final String PREFIX_FULL = "craftengine:";
    public static final String PREFIX_SHORT = "ce:";

    private final CraftEngineBlockTable table = new CraftEngineBlockTable();

    public CraftEngineBlockTable table() {
        return table;
    }

    @Override
    public String prefix() {
        return PREFIX_FULL;
    }

    @Override
    public boolean claims(String data) {
        return data.startsWith(PREFIX_FULL) || data.startsWith(PREFIX_SHORT);
    }

    @Override
    public String pluginName() {
        return "CraftEngine";
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getServer() != null && Bukkit.getPluginManager() != null && Bukkit.getPluginManager().getPlugin("CraftEngine") != null;
    }

    @Override
    public boolean isLoaded() {
        return table.isLoaded();
    }

    @Override
    public BlockState parse(String data, DepthTracker depthTracker) throws LoadException {
        String id;
        if(data.startsWith(PREFIX_FULL)) {
            id = data.substring(PREFIX_FULL.length());
        } else if(data.startsWith(PREFIX_SHORT)) {
            id = data.substring(PREFIX_SHORT.length());
        } else {
            throw new IllegalArgumentException("Not a Craft-Engine block id: " + data);
        }
        String configName = depthTracker != null ? depthTracker.getConfigurationName() : "dynamic";
        String pathDesc = depthTracker != null ? depthTracker.pathDescriptor() : "script";
        return new CraftEngineBlockState(table, id, configName, pathDesc);
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
