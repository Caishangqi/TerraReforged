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

package com.dfsek.terra.world.generation.overlay;

import com.dfsek.terra.api.world.generation.overlay.StageAction;


public final class StageComposer {
    private StageComposer() {
    }

    public static void compose(StageAction action, Runnable vanilla, Runnable terra, boolean terraConfigured) {
        if(action == null || vanilla == null || terra == null) {
            throw new IllegalArgumentException("Stage composition inputs must be present.");
        }
        switch(action) {
            case ADD -> {
                vanilla.run();
                if(terraConfigured) {
                    terra.run();
                }
            }
            case VANILLA -> vanilla.run();
            case REPLACE -> {
                if(terraConfigured) {
                    terra.run();
                }
            }
            case DISABLE -> {
            }
        }
    }
}
