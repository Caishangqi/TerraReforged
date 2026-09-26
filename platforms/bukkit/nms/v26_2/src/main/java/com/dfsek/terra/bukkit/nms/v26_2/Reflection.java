package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

import java.util.Map;


/**
 * The non-public members this plugin has to reach, and nothing else.
 * <p>
 * Every entry here is a name that must keep existing in the next Minecraft version, so the set is
 * kept as small as the job allows. Binding holder values, binding tags, creating a tag and building a
 * tag set were all removed once biome registration started closing through the registry's own
 * {@code freeze()}, which does all four.
 */
public class Reflection {
    public static final MappedRegistryProxy MAPPED_REGISTRY;
    public static final MappedRegistryTagSetProxy MAPPED_REGISTRY_TAG_SET;
    public static final StructureManagerProxy STRUCTURE_MANAGER;
    public static final ChunkMapProxy CHUNKMAP;
    public static final VillagerTypeProxy VILLAGER_TYPE;

    static {
        ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
        ReflectionProxyFactory reflectionProxyFactory = ReflectionProxyFactory.create(reflectionRemapper,
            Reflection.class.getClassLoader());

        MAPPED_REGISTRY = reflectionProxyFactory.reflectionProxy(MappedRegistryProxy.class);
        MAPPED_REGISTRY_TAG_SET = reflectionProxyFactory.reflectionProxy(MappedRegistryTagSetProxy.class);
        STRUCTURE_MANAGER = reflectionProxyFactory.reflectionProxy(StructureManagerProxy.class);
        CHUNKMAP = reflectionProxyFactory.reflectionProxy(ChunkMapProxy.class);
        VILLAGER_TYPE = reflectionProxyFactory.reflectionProxy(VillagerTypeProxy.class);
    }


    @Proxies(MappedRegistry.class)
    public interface MappedRegistryProxy {
        @FieldSetter("allTags")
        <T> void setAllTags(MappedRegistry<T> instance, Object obj);

        @FieldSetter("frozen")
        void setFrozen(MappedRegistry<?> instance, boolean frozen);

        // 26.2 split the tag state in two. `frozenTags` is what registration writes and what freeze()
        // rebuilds `allTags` from, so the two can disagree without anything noticing until a reload.
        @FieldGetter("frozenTags")
        <T> Map<TagKey<T>, HolderSet.Named<T>> getFrozenTags(MappedRegistry<T> instance);
    }


    @Proxies(className = "net.minecraft.core.MappedRegistry$TagSet")
    public interface MappedRegistryTagSetProxy {
        // MappedRegistry.freeze() refuses to run while allTags is bound. Putting the unbound set back
        // is what lets the registry's own freeze rebuild the tags rather than reimplementing it here.
        @MethodName("unbound")
        @Static
        Object invokeUnbound();
    }


    @Proxies(StructureManager.class)
    public interface StructureManagerProxy {
        @FieldGetter("level")
        LevelAccessor getLevel(StructureManager instance);
    }


    @Proxies(ChunkMap.class)
    public interface ChunkMapProxy {
        @FieldGetter("worldGenContext")
        WorldGenContext getWorldGenContext(ChunkMap instance);

        @FieldSetter("worldGenContext")
        void setWorldGenContext(ChunkMap instance, WorldGenContext worldGenContext);
    }


    @Proxies(VillagerType.class)
    public interface VillagerTypeProxy {
        @Static
        @FieldGetter("BY_BIOME")
        Map<ResourceKey<Biome>, ResourceKey<VillagerType>> getByBiome();
    }
}
