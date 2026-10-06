package com.dfsek.terra.bukkit.hooks;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.bukkit.PlatformImpl;


/**
 * Gateway for Oraxen integration.
 * Names no Oraxen classes itself so it remains safely loadable on servers without Oraxen.
 */
public final class OraxenHook implements PluginHook {
    private static final Logger logger = LoggerFactory.getLogger(OraxenHook.class);
    private static final String PROBE_CLASS = "io.th0rgal.oraxen.api.OraxenBlocks";

    @Override
    public String pluginName() {
        return "Oraxen";
    }

    @Override
    public boolean canLoad() {
        try {
            Class.forName(PROBE_CLASS);
            return true;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void register(Plugin plugin, PlatformImpl platform) {
        OraxenBlockHook.register(platform.getPlugin(), platform.getCustomBlocks().oraxen());
        OraxenBlockPlacer.register(platform.getPlugin(), platform.getCustomBlocks());
    }
}
