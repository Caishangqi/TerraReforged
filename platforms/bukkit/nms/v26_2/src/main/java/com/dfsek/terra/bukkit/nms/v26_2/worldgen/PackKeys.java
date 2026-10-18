package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Locale;

import com.dfsek.terra.api.registry.key.RegistryKey;


/**
 * Translates Terra config keys into resource locations.
 * <p>
 * A pack's config registry names its entries {@code <PACK_ID>:<id>}, and Terra allows upper case there
 * while Minecraft does not. The pack namespace therefore comes from the lower-cased pack id, which is why
 * a pack called {@code EUREKA_OVERWORLD} declares its objects under {@code eureka_overworld:}.
 */
final class PackKeys {
    private PackKeys() {
    }

    static Identifier identifier(RegistryKey key) {
        return Identifier.fromNamespaceAndPath(lower(key.getNamespace()), lower(key.getID()));
    }

    static <T> ResourceKey<T> resourceKey(ResourceKey<? extends Registry<T>> registry, RegistryKey key) {
        return ResourceKey.create(registry, identifier(key));
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
