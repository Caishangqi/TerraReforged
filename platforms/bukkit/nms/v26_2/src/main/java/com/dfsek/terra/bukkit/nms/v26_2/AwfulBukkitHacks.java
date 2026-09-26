package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.NamespacedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.bukkit.nms.v26_2.config.VanillaBiomeProperties;
import com.dfsek.terra.bukkit.world.BukkitBiomeInfo;
import com.dfsek.terra.bukkit.world.BukkitPlatformBiome;
import com.dfsek.terra.registry.master.ConfigRegistry;


/**
 * Adds every pack's Terra biomes to the server's biome registry, which has no supported way to accept
 * them, and gives each one the tags of the vanilla biome it extends.
 * <p>
 * 26.2 keeps biome tags in two places. {@code frozenTags} is what registration writes; {@code allTags}
 * is derived from it by {@link MappedRegistry#freeze()}, which also binds every holder value and
 * refreshes the tag set inside each holder. So the registry is unfrozen, written through the public
 * {@code bindTags}, and then frozen by its own {@code freeze()}. Reflection is only needed to clear
 * the two pieces of state that stop {@code freeze()} from running a second time.
 */
public class AwfulBukkitHacks {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwfulBukkitHacks.class);

    public static void registerBiomes(ConfigRegistry configRegistry) {
        LOGGER.info("Hacking biome registry...");
        MappedRegistry<Biome> biomeRegistry = (MappedRegistry<Biome>) RegistryFetcher.biomeRegistry();

        Map<ResourceKey<Biome>, Set<TagKey<Biome>>> tagsBefore = BiomeRegistryAudit.snapshotTags(biomeRegistry);
        // Vanilla biome to the Terra biomes extending it. Local rather than static: a second call would
        // otherwise add every Terra biome to its vanilla biome's tags twice over.
        Map<Identifier, List<Identifier>> terraBiomes = new HashMap<>();

        Reflection.MAPPED_REGISTRY.setFrozen(biomeRegistry, false);
        try {
            configRegistry.forEach(pack -> pack.getRegistry(com.dfsek.terra.api.world.biome.Biome.class)
                .forEach((key, biome) -> registerBiome(biomeRegistry, pack, key, biome, terraBiomes)));

            LOGGER.info("Rebuilding biome tags....");
            biomeRegistry.bindTags(pendingTags(biomeRegistry, terraBiomes));
        } catch(RuntimeException exception) {
            LOGGER.error("Biome registration failed part way through. Whatever was registered before the failure " +
                         "stays, and the check below reports what is wrong with it.", exception);
        } finally {
            refreeze(biomeRegistry);
        }

        BiomeRegistryAudit.verify(biomeRegistry, tagsBefore, terraBiomes);
    }

    /**
     * Puts the registry back in the frozen state, whatever happened while it was open.
     * <p>
     * This is in a {@code finally} because the alternative is a server running on a writable biome
     * registry full of holders that were never bound, which fails later and somewhere else.
     * {@code freeze()} throws over a bound tag set, so {@code allTags} goes back to unbound first;
     * binding holder values, rebuilding {@code allTags} from {@code frozenTags} and refreshing each
     * holder's tags is then all done by {@code freeze()} itself.
     */
    private static void refreeze(MappedRegistry<Biome> biomeRegistry) {
        try {
            Reflection.MAPPED_REGISTRY.setAllTags(biomeRegistry, Reflection.MAPPED_REGISTRY_TAG_SET.invokeUnbound());
            biomeRegistry.freeze();
        } catch(RuntimeException exception) {
            LOGGER.error("The biome registry would not freeze. Setting the flag directly so the server does not run " +
                         "on a writable registry, but biome tags and holder values are now unreliable.", exception);
            Reflection.MAPPED_REGISTRY.setFrozen(biomeRegistry, true);
        }
    }

    private static void registerBiome(MappedRegistry<Biome> biomeRegistry, ConfigPack pack, RegistryKey key,
                                      com.dfsek.terra.api.world.biome.Biome biome,
                                      Map<Identifier, List<Identifier>> terraBiomes) {
        BukkitPlatformBiome platformBiome = (BukkitPlatformBiome) biome.getPlatformBiome();

        NamespacedKey vanillaBukkitKey = platformBiome.getHandle().getKey();
        Identifier vanillaMinecraftKey = Identifier.fromNamespaceAndPath(vanillaBukkitKey.getNamespace(),
            vanillaBukkitKey.getKey());

        VanillaBiomeProperties vanillaBiomeProperties;
        try {
            vanillaBiomeProperties = biome.getContext().get(VanillaBiomeProperties.class);
        } catch(IllegalArgumentException exception) {
            // A biome only has these once its vanilla properties block loaded. Without this guard one
            // unloadable field in one biome aborts registration for every pack on the server.
            LOGGER.error("Biome {} in pack {} has no vanilla properties and is not registered. Its configuration " +
                         "failed to load; the cause is earlier in this log.", key, pack.getID());
            return;
        }

        Biome platform = NMSBiomeInjector.createBiome(biomeRegistry.get(vanillaMinecraftKey).orElseThrow().value(),
            vanillaBiomeProperties);

        Identifier delegateMinecraftKey = Identifier.fromNamespaceAndPath("terra",
            NMSBiomeInjector.createBiomeID(pack, key));
        NamespacedKey delegateBukkitKey = NamespacedKey.fromString(delegateMinecraftKey.toString());
        ResourceKey<Biome> delegateKey = ResourceKey.create(Registries.BIOME, delegateMinecraftKey);

        Reference<Biome> holder = biomeRegistry.register(delegateKey, platform, RegistrationInfo.BUILT_IN);

        platformBiome.getContext().put(new BukkitBiomeInfo(delegateBukkitKey));
        platformBiome.getContext().put(new NMSBiomeInfo(delegateKey));

        // BY_BIOME is private in 26.2, but it is a plain mutable map and VillagerType.byBiome reads it
        // directly, so putting the entry there is still how a biome gets a villager type.
        Map<ResourceKey<Biome>, ResourceKey<VillagerType>> villagerMap = Reflection.VILLAGER_TYPE.getByBiome();
        villagerMap.put(delegateKey,
            Objects.requireNonNullElse(vanillaBiomeProperties.getVillagerType(),
                villagerMap.getOrDefault(delegateKey, VillagerType.PLAINS)));

        terraBiomes.computeIfAbsent(vanillaMinecraftKey, i -> new ArrayList<>()).add(delegateKey.identifier());

        LOGGER.debug("Registered biome: {} (holder {})", delegateKey, holder.key().identifier());
    }

    /**
     * The tags to rebind, each with its current contents plus the Terra biomes that must join it.
     * <p>
     * Only tags a Terra biome joins appear here. {@code freeze()} rebuilds {@code allTags} from every
     * entry in {@code frozenTags}, so a tag left out keeps what it already had.
     */
    private static Map<TagKey<Biome>, List<Holder<Biome>>> pendingTags(MappedRegistry<Biome> registry,
                                                                       Map<Identifier, List<Identifier>> terraBiomes) {
        Map<TagKey<Biome>, List<Holder<Biome>>> pending = new HashMap<>();
        terraBiomes.forEach((vanillaId, terraIds) -> {
            Holder.Reference<Biome> vanilla = registry.get(vanillaId).orElse(null);
            if(vanilla == null) {
                LOGGER.error("No vanilla biome {}, so the Terra biomes extending it get no tags: {}", vanillaId, terraIds);
                return;
            }
            List<Holder<Biome>> terraHolders = new ArrayList<>(terraIds.size());
            for(Identifier terraId : terraIds) {
                registry.get(ResourceKey.create(Registries.BIOME, terraId))
                    .ifPresentOrElse(terraHolders::add, () -> LOGGER.error("No such biome: {}", terraId));
            }
            vanilla.tags().forEach(tag -> pending
                .computeIfAbsent(tag, t -> new ArrayList<>(currentContents(registry, t)))
                .addAll(terraHolders));
        });
        return pending;
    }

    /** A tag's members before this rebuild, so binding it again does not drop what was already there. */
    private static List<Holder<Biome>> currentContents(MappedRegistry<Biome> registry, TagKey<Biome> tag) {
        return registry.get(tag).map(named -> named.stream().toList()).orElseGet(List::of);
    }
}
