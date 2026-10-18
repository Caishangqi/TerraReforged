package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import com.dfsek.tectonic.api.config.template.annotations.Final;
import com.dfsek.tectonic.api.config.template.annotations.Value;

import java.util.Map;
import java.util.function.BiFunction;

import com.dfsek.terra.api.config.AbstractableTemplate;


/**
 * Template for a pack-declared vanilla object: an id and the object body under {@code value}.
 * <p>
 * Tectonic reads the body as nested maps and lists. Nothing here inspects it, so numbers keep the width
 * the file gave them and key names keep their case.
 */
public class RawVanillaObjectTemplate<T extends RawVanillaObject> implements AbstractableTemplate {
    private final BiFunction<String, Map<String, Object>, T> builder;

    @Value("id")
    @Final
    private String id;

    @Value("value")
    private Map<String, Object> value;

    public RawVanillaObjectTemplate(BiFunction<String, Map<String, Object>, T> builder) {
        this.builder = builder;
    }

    @Override
    public String getID() {
        return id;
    }

    public T build() {
        return builder.apply(id, value);
    }
}
