/*
 * This file is part of TerraReforged.
 *
 * TerraReforged is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TerraReforged is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TerraReforged.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.banner;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.dfsek.terra.log.TerraLogger;
import com.dfsek.terra.text.MiniMessageTexts;


/** Prints the ASCII logo and the runtime metadata lines, once, at the start of enable. */
public final class PluginBanner {
    private static final String LOGO_COLOR = "<gradient:#7bd85a:#00b7ff>";

    private final List<String> logoLines;
    private final TerraLogger logger;

    public PluginBanner(String[] logoLines, TerraLogger logger) {
        this.logoLines = List.of(Objects.requireNonNull(logoLines, "logoLines"));
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Writes the logo, a blank spacer, then the metadata lines, whose wording comes from the
     * {@code banner.*} language keys. None of them carries the prefix, because those templates do not
     * use {@code <prefix>}: the banner is one block, not a sequence of log lines.
     *
     * @param metadata values for those keys: {@code version}, {@code authors}, {@code platform} and
     *                 {@code jvm}. {@code name} is supplied by the logger.
     */
    public void print(Map<String, String> metadata) {
        Objects.requireNonNull(metadata, "metadata");

        for(String line : logoLines) {
            // A FIGlet row is art, not markup: escape it so a backslash or a bracket cannot be parsed.
            logger.component(MiniMessageTexts.deserialize(LOGO_COLOR + MiniMessageTexts.miniMessage().escapeTags(line)));
        }
        logger.component(Component.empty());
        logger.send("banner.version", metadata);
        logger.send("banner.authors", metadata);
        logger.send("banner.platform", metadata);
        logger.send("banner.jvm", metadata);
    }
}
