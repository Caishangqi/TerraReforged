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

import java.util.ArrayList;
import java.util.List;

import com.dfsek.terra.api.config.meta.Meta;
import com.dfsek.terra.api.world.generation.overlay.BiomePlacement;
import com.dfsek.terra.api.world.generation.overlay.Overlay;
import com.dfsek.terra.api.world.generation.overlay.StageAction;
import com.dfsek.terra.api.world.generation.overlay.StagePolicy;


public class OverlayTemplate implements ObjectTemplate<Overlay> {
    @Value("biome-placements")
    private @Meta List<@Meta BiomePlacement> placements;

    @Value("stage-policy.features")
    @Default
    private @Meta StageAction features = StageAction.ADD;

    @Value("stage-policy.structures")
    @Default
    private @Meta StageAction structures = StageAction.ADD;

    @Value("stage-policy.mobs")
    @Default
    private @Meta StageAction mobs = StageAction.VANILLA;

    @Override
    public Overlay get() {
        return new Overlay(new ArrayList<>(placements), new StagePolicy(features, structures, mobs));
    }
}
