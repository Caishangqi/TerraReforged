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

import ca.solostudios.strata.version.Version;
import com.dfsek.tectonic.api.exception.LoadException;
import com.dfsek.tectonic.api.loader.ConfigLoader;
import com.dfsek.tectonic.yaml.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.dfsek.terra.api.world.generation.TerrainSource;
import com.dfsek.terra.config.loaders.VersionLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TerrainSourceConfigTest {
    @Test
    public void omittedTerrainSourceLoadsAsTerra() {
        ConfigPackTemplate template = load("""
            id: TEST
            version: 1.0.0
            """);

        assertEquals(TerrainSource.TERRA, template.getTerrainSource());
    }

    @Test
    public void vanillaTerrainSourceLoads() {
        ConfigPackTemplate template = load("""
            id: TEST
            version: 1.0.0
            terrain-source: VANILLA
            """);

        assertEquals(TerrainSource.VANILLA, template.getTerrainSource());
    }

    @Test
    public void unknownTerrainSourceFailsLoading() {
        LoadException failure = assertThrows(LoadException.class, () -> load("""
            id: TEST
            version: 1.0.0
            terrain-source: NETHER
            """));

        assertTrue(failure.getMessage().contains("terrain-source"));
    }

    private static ConfigPackTemplate load(String yaml) {
        ConfigLoader loader = new ConfigLoader();
        loader.registerLoader(Version.class, new VersionLoader());
        ConfigPackTemplate template = new ConfigPackTemplate();
        loader.load(template, new YamlConfiguration(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "pack.yml"));
        return template;
    }
}
