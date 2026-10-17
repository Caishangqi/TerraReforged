/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.config.pack;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;

import java.util.LinkedHashSet;
import java.util.List;

import com.dfsek.terra.api.config.meta.Meta;
import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.NoiseSelector;
import com.dfsek.terra.api.world.generation.overlay.PlacementSelector;
import com.dfsek.terra.api.world.generation.overlay.VanillaBiomeSelector;


public class BiomePlacementTemplate implements ObjectTemplate<BiomePlacement> {
    private static final String NOISE = "NOISE";
    private static final String VANILLA_BIOME = "VANILLA_BIOME";

    @Value("id")
    private @Meta String id;

    @Value("biome")
    private @Meta String biome;

    @Value("priority")
    private @Meta int priority;

    @Value("selector.type")
    private @Meta String type;

    @Value("selector.sampler")
    @Default
    private @Meta String sampler = null;

    @Value("selector.threshold")
    @Default
    private @Meta Double threshold = null;

    @Value("selector.biomes")
    @Default
    private @Meta List<@Meta String> biomes = null;

    private int order;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public BiomePlacement get() {
        PlacementSelector selector = switch(type) {
            case NOISE -> new NoiseSelector(sampler, threshold == null ? Double.NaN : threshold);
            case VANILLA_BIOME -> new VanillaBiomeSelector(biomes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(biomes));
            default -> throw new IllegalArgumentException("Unknown placement selector type " + type + ".");
        };
        return new BiomePlacement(id, biome, priority, order, selector);
    }
}
