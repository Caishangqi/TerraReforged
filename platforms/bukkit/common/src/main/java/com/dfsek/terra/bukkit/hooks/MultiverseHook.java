package com.dfsek.terra.bukkit.hooks;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.bukkit.PlatformImpl;


/**
 * Gateway for Multiverse-Core integration.
 * Names no Multiverse classes itself so it remains safely loadable.
 */
public final class MultiverseHook implements PluginHook {
    private static final Logger logger = LoggerFactory.getLogger(MultiverseHook.class);
    private static final String PROBE_CLASS = "org.mvplugins.multiverse.core.MultiverseCoreApi";

    @Override
    public String pluginName() {
        return "Multiverse-Core";
    }

    @Override
    public boolean canLoad() {
        try {
            Class.forName(PROBE_CLASS);
            return true;
        } catch(ClassNotFoundException e) {
            logger.debug("Multiverse v5 is not installed.");
            return false;
        }
    }

    @Override
    public void register(Plugin plugin, PlatformImpl platform) {
        MultiverseGeneratorPluginHook.register(platform);
    }
}
