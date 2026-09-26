/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.bukkit;

import com.dfsek.tectonic.api.TypeRegistry;
import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import com.dfsek.terra.AbstractPlatform;
import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.handle.ItemHandle;
import com.dfsek.terra.api.handle.WorldHandle;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;
import com.dfsek.terra.bukkit.handles.BukkitItemHandle;
import com.dfsek.terra.bukkit.handles.BukkitWorldHandle;
import com.dfsek.terra.bukkit.hooks.CustomBlocks;
import com.dfsek.terra.bukkit.world.BukkitPlatformBiome;
import com.dfsek.terra.log.TerraLog;


public class PlatformImpl extends AbstractPlatform {
    private final ItemHandle itemHandle = new BukkitItemHandle();

    private final WorldHandle handle = new BukkitWorldHandle();

    // Blocks belonging to another plugin, which a pack can name but this server may not have. Created
    // here rather than by the integration that fills it, so that a pack can hold such a block on a
    // server where that integration never registers.
    private final CustomBlocks customBlocks = new CustomBlocks();

    private final TerraBukkitPlugin plugin;

    private int generationThreads;

    public PlatformImpl(TerraBukkitPlugin plugin) {
        generationThreads = getMoonriseGenerationThreadsWithReflection();
        if(generationThreads == 0) {
            generationThreads = 1;
        }
        this.plugin = plugin;
        load();
    }

    public TerraBukkitPlugin getPlugin() {
        return plugin;
    }

    public CustomBlocks getCustomBlocks() {
        return customBlocks;
    }

    @Override
    public boolean reload() {
        getTerraConfig().load(this);
        boolean succeed = loadConfigPacks();

        Bukkit.getWorlds().forEach(world -> {
            if(world.getGenerator() instanceof BukkitChunkGeneratorWrapper wrapper) {
                getConfigRegistry().get(wrapper.getPack().getRegistryKey()).ifPresent(pack -> {
                    wrapper.setPack(pack);
                    TerraLog.logger().send("platform.generator-replaced", "world", world.getName());
                });
            }
        });

        return succeed;
    }

    @Override
    public @NotNull String platformName() {
        return "Bukkit";
    }

    @Override
    public void runPossiblyUnsafeTask(@NotNull Runnable runnable) {
        plugin.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
    }

    @Override
    public @NotNull WorldHandle getWorldHandle() {
        return handle;
    }

    @Override
    public @NotNull File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public @NotNull ItemHandle getItemHandle() {
        return itemHandle;
    }

    @Override
    public int getGenerationThreads() {
        return generationThreads;
    }

    @Override
    public void register(TypeRegistry registry) {
        super.register(registry);
        registry.registerLoader(BlockState.class, (type, o, loader, depthTracker) -> parseBlockState((String) o, depthTracker))
            .registerLoader(BlockType.class, (type, o, loader, depthTracker) -> parseBlockType((String) o, depthTracker))
            .registerLoader(PlatformBiome.class, (type, o, loader, depthTracker) -> parseBiome((String) o, depthTracker))
            .registerLoader(EntityType.class, (type, o, loader, depthTracker) -> EntityType.valueOf((String) o));

    }

    protected BlockState parseBlockState(String data, DepthTracker depthTracker) throws LoadException {
        if(customBlocks.claims(data)) {
            return customBlocks.parse(data, depthTracker);
        }
        return handle.createBlockState(data);
    }

    /**
     * Only to reject a custom block with an explanation. Everything else takes the same route the
     * common loader this replaces takes.
     */
    protected BlockType parseBlockType(String data, DepthTracker depthTracker) throws LoadException {
        if(customBlocks.claims(data)) {
            throw customBlocks.notSomethingToMatchAgainst(data, depthTracker);
        }
        return handle.createBlockState(data).getBlockType();
    }

    protected BukkitPlatformBiome parseBiome(String id, DepthTracker depthTracker) throws LoadException {
        NamespacedKey key = NamespacedKey.fromString(id);
        if(key == null || !key.namespace().equals("minecraft")) throw new LoadException("Invalid biome identifier " + id, depthTracker);
        return new BukkitPlatformBiome(RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).getOrThrow(key));
    }
}
