package com.dfsek.terra.bukkit.hooks.craftengine;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.bukkit.PlatformImpl;
import com.dfsek.terra.bukkit.hooks.PluginHook;


/**
 * Gateway hook for Craft-Engine integration.
 * Names no Craft-Engine classes itself, so it remains safely loadable on servers without Craft-Engine.
 */
public final class CraftEngineHook implements PluginHook {
    private static final Logger logger = LoggerFactory.getLogger(CraftEngineHook.class);
    private static final String PROBE_CLASS = "net.momirealms.craftengine.bukkit.api.CraftEngineBlocks";

    @Override
    public String pluginName() {
        return "CraftEngine";
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
        CraftEngineBlockHook.register(plugin, platform.getCustomBlocks().craftEngine());
    }
}
