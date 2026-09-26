package com.dfsek.terra.bukkit.nms.v26_2;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.nms.v26_2.config.VanillaBiomeProperties;


public class NMSBiomeInjector {

    public static <T> Optional<Holder<T>> getEntry(Registry<T> registry, Identifier identifier) {
        return registry.getOptional(identifier)
            .flatMap(registry::getResourceKey)
            .flatMap(registry::get);
    }

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties) {
        // 26.2 moved fog, sky, ambient particles, ambient sounds, and music out of BiomeSpecialEffects
        // into the environment attribute map. BiomeSpecialEffects now only carries the block colors.
        EnvironmentAttributeMap vanillaAttributes = vanilla.getAttributes();
        EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder().putAll(vanillaAttributes);

        setIfPresent(attributes, EnvironmentAttributes.FOG_COLOR, vanillaBiomeProperties.getFogColor());
        setIfPresent(attributes, EnvironmentAttributes.WATER_FOG_COLOR, vanillaBiomeProperties.getWaterFogColor());
        setIfPresent(attributes, EnvironmentAttributes.SKY_COLOR, vanillaBiomeProperties.getSkyColor());
        setIfPresent(attributes, EnvironmentAttributes.MUSIC_VOLUME, vanillaBiomeProperties.getMusicVolume());

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            attributes.set(EnvironmentAttributes.AMBIENT_PARTICLES, List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            attributes.set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(vanillaBiomeProperties.getMusic()));
        }

        mergeAmbientSounds(vanillaAttributes, vanillaBiomeProperties)
            .ifPresent(sounds -> attributes.set(EnvironmentAttributes.AMBIENT_SOUNDS, sounds));

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
            .waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(),
                vanilla.getSpecialEffects().grassColorModifier()));

        BiomeSpecialEffects vanillaEffects = vanilla.getSpecialEffects();
        overrideColor(vanillaBiomeProperties.getGrassColor(), vanillaEffects.grassColorOverride(), effects::grassColorOverride);
        overrideColor(vanillaBiomeProperties.getFoliageColor(), vanillaEffects.foliageColorOverride(), effects::foliageColorOverride);
        overrideColor(vanillaBiomeProperties.getDryFoliageColor(), vanillaEffects.dryFoliageColorOverride(),
            effects::dryFoliageColorOverride);

        return new Biome.BiomeBuilder()
            .hasPrecipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()))
            .temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getBaseTemperature()))
            .downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(), vanilla.climateSettings.downfall()))
            .temperatureAdjustment(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(),
                vanilla.climateSettings.temperatureModifier()))
            .mobSpawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getMobSettings()))
            .putAttributes(attributes)
            .specialEffects(effects.build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }

    /**
     * Returns the ambient sounds to install, or empty when the pack overrides none of the three parts. Each part the
     * pack leaves unset keeps the value the vanilla biome resolves to.
     */
    private static Optional<AmbientSounds> mergeAmbientSounds(EnvironmentAttributeMap vanillaAttributes,
                                                              VanillaBiomeProperties properties) {
        SoundEvent loop = properties.getLoopSound();
        AmbientMoodSettings mood = properties.getMoodSound();
        AmbientAdditionsSettings additions = properties.getAdditionsSound();

        if(loop == null && mood == null && additions == null) {
            return Optional.empty();
        }

        AmbientSounds inherited = effectiveValue(vanillaAttributes, EnvironmentAttributes.AMBIENT_SOUNDS);

        return Optional.of(new AmbientSounds(
            loop == null ? inherited.loop() : soundHolder(loop),
            mood == null ? inherited.mood() : Optional.of(mood),
            additions == null ? inherited.additions() : List.of(additions)));
    }

    private static Optional<Holder<SoundEvent>> soundHolder(SoundEvent sound) {
        return RegistryFetcher.soundEventRegistry().get(sound.location()).map(reference -> (Holder<SoundEvent>) reference);
    }

    /**
     * An attribute map only stores modifiers, so the value a biome actually resolves to is the attribute default with
     * that biome's modifier applied.
     */
    private static <V> V effectiveValue(EnvironmentAttributeMap attributes, EnvironmentAttribute<V> attribute) {
        return attributes.applyModifier(attribute, attribute.defaultValue());
    }

    private static <V> void setIfPresent(EnvironmentAttributeMap.Builder attributes, EnvironmentAttribute<V> attribute, V value) {
        if(value != null) {
            attributes.set(attribute, value);
        }
    }

    private static void overrideColor(Integer configured, Optional<Integer> inherited, java.util.function.IntConsumer apply) {
        if(configured == null) {
            inherited.ifPresent(apply::accept);
        } else {
            apply.accept(configured);
        }
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }
}
