package com.dfsek.terra.bukkit.hooks;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import com.dfsek.terra.bukkit.PlatformImpl;


/**
 * Manages external plugin lifecycle hooks behind safe probe boundaries using Java ServiceLoader SPI.
 */
public final class HookManager {
    private static final Logger logger = LoggerFactory.getLogger(HookManager.class);

    private final List<PluginHook> hooks = new ArrayList<>();

    public HookManager() {
        ServiceLoader.load(PluginHook.class, HookManager.class.getClassLoader())
            .forEach(hooks::add);
        logger.debug("Discovered {} plugin hook(s) via ServiceLoader.", hooks.size());
    }

    public void registerHook(PluginHook hook) {
        hooks.add(hook);
    }

    public void onPluginEnable(Plugin plugin, PlatformImpl platform) {
        String name = plugin.getName();
        for(PluginHook hook : hooks) {
            if(hook.pluginName().equals(name)) {
                if(hook.canLoad()) {
                    logger.debug("Initializing hook for {}", name);
                    try {
                        hook.register(plugin, platform);
                    } catch(Exception e) {
                        logger.error("Failed to register hook for {}.", name, e);
                    }
                } else {
                    logger.error("A plugin called {} is enabled, but its API is not the one Terra was built against.", name);
                }
            }
        }
    }
}
