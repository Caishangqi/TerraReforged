package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Checks that rewriting the biome registry left it consistent, and says so out loud.
 * <p>
 * Registry injection fails quietly. A tag that lost its members, a holder bound to nothing, or a Terra
 * biome the registry can see but no tag contains all produce a server that starts normally and
 * generates a world that is subtly wrong. Nothing in a log line saying "Hacking biome registry..."
 * distinguishes those from success, so this reads the result back and compares it against what was
 * there before.
 * <p>
 * It runs once per server start over roughly a thousand biomes, and reports only counts unless
 * something is wrong.
 */
final class BiomeRegistryAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeRegistryAudit.class);
    /** Enough failures to show the shape of the problem without filling the log with the same one. */
    private static final int EXAMPLES = 5;

    private BiomeRegistryAudit() {
    }

    /** Every biome's tag membership before the registry is touched, so tag loss can be detected. */
    static Map<ResourceKey<Biome>, Set<TagKey<Biome>>> snapshotTags(MappedRegistry<Biome> registry) {
        Map<ResourceKey<Biome>, Set<TagKey<Biome>>> before = new HashMap<>();
        registry.listElements().forEach(holder -> tagsOf(holder).ifPresent(tags -> before.put(holder.key(), tags)));
        return before;
    }

    /**
     * A holder's tags, or nothing when it has none bound.
     * <p>
     * {@link Holder.Reference#tags()} throws on a holder whose tags were never bound, which is exactly
     * the state this class exists to find. Letting that escape would abort the audit, and because the
     * audit runs inside Terra's enable event, it would take the plugin down with it. A check that can
     * break the server it is checking is worse than no check.
     */
    private static Optional<Set<TagKey<Biome>>> tagsOf(Holder.Reference<Biome> holder) {
        try {
            return Optional.of(holder.tags().collect(Collectors.toSet()));
        } catch(RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * @param terraBiomes vanilla biome id to the Terra biome ids registered as extending it
     */
    static void verify(MappedRegistry<Biome> registry,
                       Map<ResourceKey<Biome>, Set<TagKey<Biome>>> before,
                       Map<Identifier, List<Identifier>> terraBiomes) {
        Problems problems = new Problems();
        // Counted and logged because "the map and byBiome agree" passes just as well when every biome
        // silently fell back to the default. Seeing a configured type in the tally is the actual proof.
        Map<Identifier, Integer> villagerTypes = new HashMap<>();
        int terraCount = 0;
        try {
            for(Map.Entry<Identifier, List<Identifier>> entry : terraBiomes.entrySet()) {
                Optional<Holder.Reference<Biome>> vanilla = registry.get(entry.getKey());
                if(vanilla.isEmpty()) {
                    problems.add("vanilla biome missing", entry.getKey().toString());
                    continue;
                }
                Set<TagKey<Biome>> vanillaTags = tagsOf(vanilla.get()).orElseGet(() -> {
                    problems.add("vanilla biome has no tags bound", entry.getKey().toString());
                    return Set.of();
                });
                for(Identifier terraId : entry.getValue()) {
                    terraCount++;
                    checkTerraBiome(registry, terraId, vanillaTags, problems, villagerTypes);
                }
            }

            checkNoTagWasLost(registry, before, problems);
            checkTagSetsAgree(registry, problems);
            checkFrozenTagsMatchAllTags(registry, problems);
        } catch(RuntimeException exception) {
            LOGGER.error("Biome registry check could not finish. Treat the registry as unverified.", exception);
            return;
        }

        if(problems.isEmpty()) {
            LOGGER.info("Biome registry verified: {} Terra biomes registered across {} vanilla biomes, " +
                        "{} entries and {} tags intact. Villager types in use: {}",
                terraCount, terraBiomes.size(), registry.size(), registry.getTags().count(), villagerTypes);
        } else {
            LOGGER.error("Biome registry injection left {} problems. Terra biomes will not behave like the vanilla " +
                         "biomes they extend.", problems.total());
            problems.report();
        }
    }

    private static void checkTerraBiome(MappedRegistry<Biome> registry, Identifier terraId,
                                        Set<TagKey<Biome>> vanillaTags, Problems problems,
                                        Map<Identifier, Integer> villagerTypes) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, terraId);
        Optional<Holder.Reference<Biome>> found = registry.get(key);
        if(found.isEmpty()) {
            problems.add("Terra biome not in registry", terraId.toString());
            return;
        }
        Holder.Reference<Biome> holder = found.get();

        // register() does not bind the value in 26.2; something after it has to. An unbound holder
        // throws only when a chunk asks for the biome, long after the cause is gone from the log.
        if(!holder.isBound()) {
            problems.add("holder not bound", terraId.toString());
            return;
        }

        Set<TagKey<Biome>> terraTags = tagsOf(holder).orElse(null);
        if(terraTags == null) {
            problems.add("holder has no tags bound", terraId.toString());
            return;
        }
        Set<TagKey<Biome>> missing = new HashSet<>(vanillaTags);
        missing.removeAll(terraTags);
        if(!missing.isEmpty()) {
            problems.add("Terra biome missing its vanilla biome's tags",
                terraId + " lacks " + missing.stream().map(t -> t.location().toString()).sorted().toList());
        }

        ResourceKey<VillagerType> villager = Reflection.VILLAGER_TYPE.getByBiome().get(key);
        if(villager == null) {
            problems.add("no villager type mapped", terraId.toString());
        } else {
            villagerTypes.merge(villager.identifier(), 1, Integer::sum);
        }
        if(villager != null && !villager.equals(VillagerType.byBiome(holder))) {
            // VillagerType.BY_BIOME is private in 26.2. Writing it reflectively and then reading back
            // through the public accessor is the only proof the write is the one vanilla will use.
            problems.add("villager type not visible through VillagerType.byBiome",
                terraId + " mapped to " + villager.identifier() + " but byBiome returns "
                + VillagerType.byBiome(holder).identifier());
        }
    }

    private static void checkNoTagWasLost(MappedRegistry<Biome> registry,
                                          Map<ResourceKey<Biome>, Set<TagKey<Biome>>> before, Problems problems) {
        before.forEach((key, tags) -> {
            Optional<Holder.Reference<Biome>> holder = registry.get(key);
            if(holder.isEmpty()) {
                problems.add("biome disappeared from the registry", key.identifier().toString());
                return;
            }
            Set<TagKey<Biome>> missing = new HashSet<>(tags);
            tagsOf(holder.get()).orElse(Set.of()).forEach(missing::remove);
            if(!missing.isEmpty()) {
                problems.add("biome lost tags it had before injection",
                    key.identifier() + " lost " + missing.stream().map(t -> t.location().toString()).sorted().toList());
            }
        });
    }

    /**
     * Tag membership is stored twice: inside each holder, and inside the tag's {@link HolderSet.Named}.
     * Rebuilding one and not the other is the failure mode that makes a biome answer {@code is(tag)}
     * correctly while every tag-driven lookup skips it.
     */
    private static void checkTagSetsAgree(MappedRegistry<Biome> registry, Problems problems) {
        registry.getTags().forEach(named -> {
            List<Holder<Biome>> contents;
            try {
                // contents() throws rather than returning empty when a tag was never bound, and that
                // is the interesting case, so the exception is the finding.
                contents = named.stream().toList();
            } catch(RuntimeException exception) {
                problems.add("tag is unbound", named.key().location().toString());
                return;
            }
            for(Holder<Biome> holder : contents) {
                if(!(holder instanceof Holder.Reference<Biome> reference) || tagsOf(reference).isEmpty()
                   || !holder.is(named.key())) {
                    problems.add("tag contains a holder that does not know it",
                        named.key().location() + " contains " + holder.getRegisteredName());
                }
            }
        });
    }

    private static void checkFrozenTagsMatchAllTags(MappedRegistry<Biome> registry, Problems problems) {
        Map<TagKey<Biome>, HolderSet.Named<Biome>> frozen = Reflection.MAPPED_REGISTRY.getFrozenTags(registry);
        Map<TagKey<Biome>, HolderSet.Named<Biome>> all = registry.getTags()
            .collect(Collectors.toMap(HolderSet.Named::key, named -> named, (a, b) -> a));

        for(Map.Entry<TagKey<Biome>, HolderSet.Named<Biome>> entry : all.entrySet()) {
            HolderSet.Named<Biome> counterpart = frozen.get(entry.getKey());
            if(counterpart == null) {
                problems.add("tag is in allTags but not frozenTags", entry.getKey().location().toString());
            } else if(counterpart != entry.getValue()) {
                problems.add("allTags and frozenTags hold different objects for a tag",
                    entry.getKey().location().toString());
            }
        }
        for(TagKey<Biome> key : frozen.keySet()) {
            if(!all.containsKey(key)) {
                problems.add("tag is in frozenTags but not allTags", key.location().toString());
            }
        }
    }

    /** Groups failures by kind so one broken tag does not hide a different problem further down. */
    private static final class Problems {
        private final Map<String, List<String>> byKind = new HashMap<>();
        private int total;

        void add(String kind, String detail) {
            total++;
            byKind.computeIfAbsent(kind, k -> new ArrayList<>()).add(detail);
        }

        boolean isEmpty() {
            return total == 0;
        }

        int total() {
            return total;
        }

        void report() {
            byKind.forEach((kind, details) -> {
                LOGGER.error("  {}: {}", kind, details.size());
                details.stream().limit(EXAMPLES).forEach(detail -> LOGGER.error("      {}", detail));
                if(details.size() > EXAMPLES) {
                    LOGGER.error("      ... and {} more", details.size() - EXAMPLES);
                }
            });
        }
    }
}
