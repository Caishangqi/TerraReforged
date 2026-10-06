package com.dfsek.terra.bukkit.hooks;

import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import com.dfsek.terra.bukkit.hooks.craftengine.CraftEngineBlockState;
import com.dfsek.terra.bukkit.hooks.craftengine.CraftEngineBlockTable;
import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CraftEngineBlockStateTest {
    private static final String ID = "default:palm_log";

    private final CraftEngineBlockTable table = new CraftEngineBlockTable();
    private final CraftEngineBlockState state = new CraftEngineBlockState(table, ID, "trees.yml", "palette.layers[0]");

    @Test
    void theDeferredBlockIsTheConcreteTypeTheWritePathsCastTo() {
        assertTrue(BukkitBlockState.class.isAssignableFrom(CraftEngineBlockState.class));
    }

    @Test
    void beforeCraftEngineHasLoadedTheFailureSaysThat() {
        assertFalse(table.isLoaded());

        String message = assertThrows(IllegalStateException.class, state::getHandle).getMessage();

        assertTrue(message.contains("has not loaded its blocks yet"), message);
        assertTrue(message.contains(ID), message);
        assertTrue(message.contains("trees.yml"), message);
        assertTrue(message.contains("palette.layers[0]"), message);
    }

    @Test
    void anIdCraftEngineDoesNotHaveIsADifferentFailure() {
        table.replace(Map.of("something_else", blockData()));

        String message = assertThrows(IllegalStateException.class, state::getHandle).getMessage();

        assertTrue(message.contains("no block called"), message);
        assertTrue(message.contains(ID), message);
        assertTrue(message.contains("trees.yml"), message);
    }

    @Test
    void aKnownIdResolvesToCraftEnginesBlockData() {
        BlockData data = blockData();
        table.replace(Map.of(ID, data));

        assertSame(data, state.getHandle());
    }

    @Test
    void aReloadedTableIsVisibleToABlockThatAlreadyResolved() {
        table.replace(Map.of(ID, blockData()));
        state.getHandle();

        BlockData afterReload = blockData();
        table.replace(Map.of(ID, afterReload));

        assertSame(afterReload, state.getHandle());
    }

    private static BlockData blockData() {
        return (BlockData) Proxy.newProxyInstance(
            BlockData.class.getClassLoader(),
            new Class<?>[]{ BlockData.class },
            (proxy, method, args) -> switch(method.getName()) {
                case "toString" -> "BlockData stub " + System.identityHashCode(proxy);
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
