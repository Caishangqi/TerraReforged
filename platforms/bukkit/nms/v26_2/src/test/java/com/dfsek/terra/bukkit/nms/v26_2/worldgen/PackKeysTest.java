package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import com.dfsek.terra.api.registry.key.RegistryKey;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PackKeysTest {
    @Test
    public void packConfigKeysBecomeLowercaseResourceLocations() {
        assertEquals("eureka_overworld:birch_trees_forest",
            PackKeys.identifier(RegistryKey.of("EUREKA_OVERWORLD", "BIRCH_TREES_FOREST")).toString());
    }

    @Test
    public void serverKeysKeepTheirNamespace() {
        assertEquals("minecraft:amethyst_geode",
            PackKeys.identifier(RegistryKey.parse("minecraft:amethyst_geode")).toString());
    }

    @Test
    public void resourceKeysUseTheRequestedRegistry() {
        ResourceKey<?> key = PackKeys.resourceKey(Registries.PLACED_FEATURE, RegistryKey.parse("minecraft:amethyst_geode"));

        assertEquals("minecraft:amethyst_geode", key.identifier().toString());
        assertEquals(Registries.PLACED_FEATURE, key.registryKey());
    }
}
