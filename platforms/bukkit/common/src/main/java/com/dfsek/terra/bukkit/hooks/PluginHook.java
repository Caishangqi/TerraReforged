package com.dfsek.terra.bukkit.hooks;

import org.bukkit.plugin.Plugin;

import com.dfsek.terra.bukkit.PlatformImpl;


/**
 * Gateway hook for an external Bukkit plugin integration.
 * <p>
 * Implementations of this interface must NEVER directly import or reference classes
 * from the targeted external plugin, keeping this gateway class safe to load on servers
 * where the external plugin is absent.
 */
public interface PluginHook {
    /**
     * The plugin name as reported by Bukkit (e.g. "Multiverse-Core", "Oraxen", "CraftEngine").
     */
    String pluginName();

    /**
     * Probes whether the required external API classes exist before any implementation
     * class referencing them is loaded.
     */
    boolean canLoad();

    /**
     * Registers this hook's internal listeners and components with the platform.
     * Called only after {@link #canLoad()} returns true.
     */
    void register(Plugin plugin, PlatformImpl platform);
}
