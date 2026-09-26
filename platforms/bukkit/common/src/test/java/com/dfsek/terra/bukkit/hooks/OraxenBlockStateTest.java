package com.dfsek.terra.bukkit.hooks;

import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The deferred block is the one piece of the Oraxen integration whose failure modes can be reached
 * without a server: it resolves through a table rather than through Oraxen itself. Everything checked
 * here is something a compiler cannot catch.
 */
class OraxenBlockStateTest {
    private static final String ID = "amethyst_ore";

    private final OraxenBlockTable table = new OraxenBlockTable();
    private final OraxenBlockState state = new OraxenBlockState(table, ID, "ores.yml", "palette.layers[0]");

    @Test
    void theDeferredBlockIsTheConcreteTypeTheWritePathsCastTo() {
        // NMSChunkGeneratorDelegate casts to BukkitBlockState on its way to CraftBlockData, so an
        // implementation written beside it instead of under it compiles and then fails at generation.
        assertTrue(BukkitBlockState.class.isAssignableFrom(OraxenBlockState.class));
    }

    @Test
    void beforeOraxenHasLoadedTheFailureSaysThat() {
        assertFalse(table.isLoaded());

        String message = assertThrows(IllegalStateException.class, state::getHandle).getMessage();

        assertTrue(message.contains("has not loaded its blocks yet"), message);
        assertTrue(message.contains(ID), message);
        assertTrue(message.contains("ores.yml"), message);
        assertTrue(message.contains("palette.layers[0]"), message);
    }

    @Test
    void anIdOraxenDoesNotHaveIsADifferentFailure() {
        table.replace(Map.of("something_else", blockData()));

        String message = assertThrows(IllegalStateException.class, state::getHandle).getMessage();

        assertTrue(message.contains("no block called"), message);
        assertTrue(message.contains(ID), message);
        assertTrue(message.contains("ores.yml"), message);
    }

    @Test
    void aKnownIdResolvesToOraxensBlockData() {
        BlockData data = blockData();
        table.replace(Map.of(ID, data));

        assertSame(data, state.getHandle());
    }

    @Test
    void aReloadedTableIsVisibleToABlockThatAlreadyResolved() {
        // /oraxen reload rebuilds the mechanic factories every earlier answer came from. A handle
        // cached in this object would outlive the factory that produced it.
        table.replace(Map.of(ID, blockData()));
        state.getHandle();

        BlockData afterReload = blockData();
        table.replace(Map.of(ID, afterReload));

        assertSame(afterReload, state.getHandle());
    }

    /**
     * A distinct BlockData with no server behind it. Only identity is ever asked of it.
     */
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
