package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.bukkit.nms.v26_2.Reflection;
import com.dfsek.terra.bukkit.nms.v26_2.RegistryFetcher;


/**
 * Decodes the vanilla world generation objects a pack declares.
 * <p>
 * Decoding order follows the references: noise, then configured features, placed features, carvers, and the
 * surface rule. Every decoded object goes into a pack-local registry before the next kind is read, so a
 * reference written with the pack namespace resolves to the pack's own object and anything else falls back
 * to the server registry.
 * <p>
 * Noise parameters are the one thing written into a server registry. {@code RandomState} resolves them from
 * the {@code NOISE} registry, and the surface rule cannot be evaluated without that. Only new keys are added;
 * an existing key is reported and left alone.
 */
public final class WorldgenDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldgenDataLoader.class);
    private static final Gson GSON = new Gson();
    // Packs load on parallel threads and the NOISE registry is shared, so its unfreeze, write and freeze
    // have to happen one pack at a time.
    private static final Object NOISE_REGISTRY_LOCK = new Object();

    private final ConfigPack pack;
    private final String namespace;
    private final RegistryOps<JsonElement> ops;
    private final MappedRegistry<ConfiguredFeature<?, ?>> configuredFeatureRegistry;
    private final MappedRegistry<PlacedFeature> placedFeatureRegistry;
    private final MappedRegistry<ConfiguredWorldCarver<?>> carverRegistry;
    private final Map<ResourceKey<? extends Registry<?>>, Registry<?>> packRegistries = new LinkedHashMap<>();

    public WorldgenDataLoader(ConfigPack pack) {
        this.pack = pack;
        this.namespace = PackKeys.identifier(pack.getRegistryKey()).getNamespace();
        this.configuredFeatureRegistry = new MappedRegistry<>(Registries.CONFIGURED_FEATURE, Lifecycle.stable());
        this.placedFeatureRegistry = new MappedRegistry<>(Registries.PLACED_FEATURE, Lifecycle.stable());
        this.carverRegistry = new MappedRegistry<>(Registries.CONFIGURED_CARVER, Lifecycle.stable());
        packRegistries.put(Registries.CONFIGURED_FEATURE, configuredFeatureRegistry);
        packRegistries.put(Registries.PLACED_FEATURE, placedFeatureRegistry);
        packRegistries.put(Registries.CONFIGURED_CARVER, carverRegistry);
        this.ops = RegistryOps.create(JsonOps.INSTANCE,
            new PackLookupProvider(MinecraftServer.getServer().registryAccess(), packRegistries));
    }

    public static PackWorldgen load(ConfigPack pack) {
        return new WorldgenDataLoader(pack).load();
    }

    public PackWorldgen load() {
        registerNoise();
        decodeAll();
        return new PackWorldgen(configuredFeatureRegistry, placedFeatureRegistry, carverRegistry, decodeSurfaceRule());
    }

    private void registerNoise() {
        Map<RegistryKey, RawVanillaObjects.Noise> declared = declared(RawVanillaObjects.Noise.class);
        if(declared.isEmpty()) {
            return;
        }

        MappedRegistry<NormalNoise.NoiseParameters> registry =
            (MappedRegistry<NormalNoise.NoiseParameters>) RegistryFetcher.noiseRegistry();
        List<RegistryKey> added = new ArrayList<>();

        synchronized(NOISE_REGISTRY_LOCK) {
            Reflection.MAPPED_REGISTRY.setFrozen(registry, false);
            try {
                for(Map.Entry<RegistryKey, RawVanillaObjects.Noise> entry : declared.entrySet()) {
                    ResourceKey<NormalNoise.NoiseParameters> key = PackKeys.resourceKey(Registries.NOISE, entry.getKey());
                    if(registry.containsKey(key)) {
                        LOGGER.warn("Pack {} declares noise {} which the server registry already holds. Keeping the server entry.",
                            pack.getID(), key.identifier());
                        continue;
                    }
                    registry.register(key, decode(entry.getValue(), NormalNoise.NoiseParameters.DIRECT_CODEC),
                        RegistrationInfo.BUILT_IN);
                    added.add(entry.getKey());
                }
            } finally {
                // freeze() rejects a bound tag set, and the registry arrives frozen with its tags already bound.
                Reflection.MAPPED_REGISTRY.setAllTags(registry, Reflection.MAPPED_REGISTRY_TAG_SET.invokeUnbound());
                registry.freeze();
            }
        }

        if(!added.isEmpty()) {
            LOGGER.info("Registered {} noise parameter sets from pack {}.", added.size(), pack.getID());
        }
    }

    /**
     * Decodes every declared object, retrying those that name a pack object which is not decoded yet.
     * <p>
     * 26.2 lets a configured feature hold placed features and a placed feature hold a configured one, so
     * neither kind can be decoded in one pass over the other. A failed attempt registers nothing, which is
     * what makes a retry pass safe. A failure that does not name a pack object is real and propagates at once.
     */
    private void decodeAll() {
        List<Declared> pending = new ArrayList<>();
        declared(RawVanillaObjects.ConfiguredFeature.class).forEach((key, raw) ->
            pending.add(new Declared(Kind.CONFIGURED_FEATURE, key, raw)));
        declared(RawVanillaObjects.PlacedFeature.class).forEach((key, raw) ->
            pending.add(new Declared(Kind.PLACED_FEATURE, key, raw)));
        declared(RawVanillaObjects.Carver.class).forEach((key, raw) ->
            pending.add(new Declared(Kind.CARVER, key, raw)));

        RuntimeException unresolved = null;
        while(!pending.isEmpty()) {
            boolean progress = false;
            for(Iterator<Declared> iterator = pending.iterator(); iterator.hasNext(); ) {
                Declared declared = iterator.next();
                try {
                    decodeOne(declared);
                    iterator.remove();
                    progress = true;
                } catch(RuntimeException exception) {
                    if(exception.getMessage() == null || !exception.getMessage().contains(namespace + ":")) {
                        throw exception;
                    }
                    unresolved = exception;
                }
            }
            if(!progress) {
                throw new IllegalArgumentException("Could not decode " + pending.size() +
                                                   " world generation objects because their pack references stay unresolved: " +
                                                   unresolved.getMessage(), unresolved);
            }
        }

        configuredFeatureRegistry.freeze();
        placedFeatureRegistry.freeze();
        carverRegistry.freeze();
    }

    private void decodeOne(Declared declared) {
        switch(declared.kind()) {
            case CONFIGURED_FEATURE -> configuredFeatureRegistry.register(
                PackKeys.resourceKey(Registries.CONFIGURED_FEATURE, declared.key()),
                decode(declared.raw(), ConfiguredFeature.DIRECT_CODEC), RegistrationInfo.BUILT_IN);
            case PLACED_FEATURE -> placedFeatureRegistry.register(
                PackKeys.resourceKey(Registries.PLACED_FEATURE, declared.key()),
                decode(declared.raw(), PlacedFeature.DIRECT_CODEC), RegistrationInfo.BUILT_IN);
            case CARVER -> carverRegistry.register(
                PackKeys.resourceKey(Registries.CONFIGURED_CARVER, declared.key()),
                decode(declared.raw(), ConfiguredWorldCarver.DIRECT_CODEC), RegistrationInfo.BUILT_IN);
        }
    }

    private enum Kind {
        CONFIGURED_FEATURE,
        PLACED_FEATURE,
        CARVER
    }

    private record Declared(Kind kind, RegistryKey key, RawVanillaObject raw) {
    }

    private SurfaceRules.RuleSource decodeSurfaceRule() {
        Map<RegistryKey, RawVanillaObjects.SurfaceRule> declared = declared(RawVanillaObjects.SurfaceRule.class);
        if(declared.isEmpty()) {
            return null;
        }

        RegistryKey selected = pack.surfaceRule()
            .map(id -> RegistryKey.of(pack.getRegistryKey().getNamespace(), id))
            .orElseGet(() -> {
                if(declared.size() > 1) {
                    throw new IllegalArgumentException("Pack " + pack.getID() + " declares " + declared.size() +
                                                       " surface rules and pack.yml does not name one with surface-rule.");
                }
                return declared.keySet().iterator().next();
            });

        RawVanillaObjects.SurfaceRule raw = declared.get(selected);
        if(raw == null) {
            throw new IllegalArgumentException("pack.yml names surface rule \"" + selected.getID() + "\" but pack " +
                                               pack.getID() + " declares " + declared.keySet());
        }
        return decode(raw, SurfaceRules.RuleSource.CODEC);
    }

    private <T extends RawVanillaObject> Map<RegistryKey, T> declared(Class<T> type) {
        Map<RegistryKey, T> declared = new LinkedHashMap<>();
        pack.getRegistry(type).forEach(declared::put);
        return declared;
    }

    private <T> T decode(RawVanillaObject raw, Codec<T> codec) {
        JsonElement json = GSON.toJsonTree(raw.value());
        DataResult<T> result = codec.parse(ops, json);
        List<String> errors = new ArrayList<>();
        Optional<T> decoded = result.resultOrPartial(errors::add);
        return decoded.orElseThrow(() -> new IllegalArgumentException(
            "Could not decode \"" + raw.id() + "\" as a vanilla world generation object: " + String.join("; ", errors)));
    }

    /**
     * Serves the pack registries first and the server registries second, entry by entry.
     * <p>
     * Returning the pack registry on its own would hide every server entry of that kind, which breaks a
     * feature that names a vanilla object the pack does not declare. The lookup adapter caches the lookup
     * per registry key, not its contents, so entries registered later stay visible.
     */
    private record PackFirstLookup<T>(HolderLookup.RegistryLookup<T> local,
                                      HolderLookup.RegistryLookup<T> server) implements HolderLookup.RegistryLookup<T> {
        @Override
        @SuppressWarnings("unchecked")
        public ResourceKey<? extends Registry<T>> key() {
            return (ResourceKey<? extends Registry<T>>) (ResourceKey<?>) local.key();
        }

        @Override
        public Optional<T> getValueForCopying(ResourceKey<T> key) {
            return server.getValueForCopying(key);
        }

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            Optional<Holder.Reference<T>> found = local.get(key);
            return found.isPresent() ? found : server.get(key);
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
            return server.get(tag);
        }

        @Override
        public Stream<Holder.Reference<T>> listElements() {
            return Stream.concat(local.listElements(), server.listElements());
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            return server.listTags();
        }

        @Override
        public Lifecycle registryLifecycle() {
            return server.registryLifecycle();
        }
    }

    private static final class PackLookupProvider implements HolderLookup.Provider {
        private final HolderLookup.Provider server;
        private final Map<ResourceKey<? extends Registry<?>>, Registry<?>> pack;

        private PackLookupProvider(HolderLookup.Provider server, Map<ResourceKey<? extends Registry<?>>, Registry<?>> pack) {
            this.server = server;
            this.pack = pack;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(
            ResourceKey<? extends Registry<? extends T>> key) {
            Registry<?> local = pack.get(key);
            Optional<? extends HolderLookup.RegistryLookup<T>> inherited = server.lookup(key);
            if(local == null) {
                return inherited;
            }
            if(inherited.isEmpty()) {
                return Optional.of((Registry<T>) local);
            }
            return Optional.of(new PackFirstLookup<>((Registry<T>) local, inherited.get()));
        }

        @Override
        public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
            return Stream.concat(server.listRegistryKeys(), pack.keySet().stream()).distinct();
        }
    }
}
