package com.dfsek.terra.bukkit.hooks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Which block strings belong to another plugin. Everything past that decision needs a running server,
 * so this covers the decision itself.
 */
class CustomBlocksTest {
    private final CustomBlocks customBlocks = new CustomBlocks();

    @Test
    void onlyAnOraxenOrCraftEnginePrefixIsClaimed() {
        assertTrue(customBlocks.claims("oraxen:amethyst_ore"));
        assertTrue(customBlocks.claims("craftengine:default:palm_log"));
        assertTrue(customBlocks.claims("ce:default:palm_log"));
        assertFalse(customBlocks.claims("minecraft:note_block"));
        assertFalse(customBlocks.claims("note_block"));
        // A prefix, not a substring, and not a word that merely begins the same way.
        assertFalse(customBlocks.claims("minecraft:oraxen:thing"));
        assertFalse(customBlocks.claims("oraxenite"));
        assertFalse(customBlocks.claims("minecraft:craftengine:thing"));
    }

    @Test
    void parsingAStringItDoesNotClaimIsAProgrammingError() {
        // Not a LoadException: a pack cannot cause this, only a caller that skipped claims().
        assertThrows(IllegalArgumentException.class, () -> customBlocks.parse("minecraft:stone", null));
    }
}
