package com.dfsek.terra.bukkit.nms.v26_2.worldgen;

import java.util.Map;


/**
 * A vanilla world generation object as a pack declares it, before decoding.
 * <p>
 * The payload stays a plain map so the decoder sees exactly what the file said. Rewriting it into an
 * intermediate shape here would make the round-trip check meaningless.
 */
public interface RawVanillaObject {
    String id();

    Map<String, Object> value();
}
