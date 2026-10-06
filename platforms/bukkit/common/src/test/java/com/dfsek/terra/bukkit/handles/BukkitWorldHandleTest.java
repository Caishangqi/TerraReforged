package com.dfsek.terra.bukkit.handles;

import org.junit.jupiter.api.Test;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.bukkit.hooks.CustomBlockProvider;
import com.dfsek.terra.bukkit.hooks.CustomBlocks;
import com.dfsek.terra.bukkit.hooks.craftengine.CraftEngineBlockState;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BukkitWorldHandleTest {
    @Test
    void uninstalledPluginThrowsHelpfulException() {
        CustomBlocks customBlocks = new CustomBlocks();
        BukkitWorldHandle handle = new BukkitWorldHandle(customBlocks);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> handle.createBlockState("craftengine:default:palm_log"));
        assertTrue(thrown.getMessage().contains("CraftEngine"), thrown.getMessage());
    }

    @Test
    void customProviderParsesSuccessfully() {
        CustomBlocks customBlocks = new CustomBlocks();
        // Register an installed mock provider
        customBlocks.registerProvider(new CustomBlockProvider() {
            @Override
            public String prefix() {
                return "test:";
            }

            @Override
            public String pluginName() {
                return "TestPlugin";
            }

            @Override
            public boolean isInstalled() {
                return true;
            }

            @Override
            public boolean isLoaded() {
                return true;
            }

            @Override
            public BlockState parse(String data, com.dfsek.tectonic.api.depth.DepthTracker depthTracker) {
                return customBlocks.craftEngineProvider().parse("craftengine:default:palm_log", depthTracker);
            }

            @Override
            public java.util.Optional<org.bukkit.block.data.BlockData> blockData(String id) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Collection<org.bukkit.block.data.BlockData> allBlockData() {
                return java.util.List.of();
            }
        });

        BukkitWorldHandle handle = new BukkitWorldHandle(customBlocks);
        BlockState state = handle.createBlockState("test:foo");
        assertNotNull(state);
        assertInstanceOf(CraftEngineBlockState.class, state);
    }
}
