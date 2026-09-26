package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import com.dfsek.terra.api.properties.Properties;


public record NMSBiomeInfo(ResourceKey<Biome> biomeKey) implements Properties {
}
