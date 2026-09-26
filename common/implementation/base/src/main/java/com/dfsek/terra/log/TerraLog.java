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

package com.dfsek.terra.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import com.dfsek.terra.lang.Messages;
import com.dfsek.terra.text.MiniMessageTexts;


/**
 * The install point and accessor for the styled console logger.
 * <p>
 * This is static for the same reason every class in this tree holds a {@code static final Logger}:
 * logging is the one cross-cutting concern here that is already reached statically, and threading a
 * second logger through {@code AbstractPlatform}, every platform and every addon constructor would be
 * a wide change that buys no behaviour.
 * <p>
 * Before a platform calls {@link #install}, and on a platform that never does, lines are flattened to
 * plain text and written through SLF4J, so no message is lost and nothing has to check for null.
 */
public final class TerraLog {
    private static final Logger FALLBACK = LoggerFactory.getLogger("TerraReforged");

    private static volatile TerraLogger logger;

    private TerraLog() {
    }

    /**
     * Binds the logger to a platform console. Called once per enable, and again on a reload that
     * re-reads the language file.
     */
    public static void install(String pluginName, ConsoleSink sink, Messages messages) {
        logger = new TerraLogger(Objects.requireNonNull(pluginName, "pluginName"),
            Objects.requireNonNull(sink, "sink"),
            Objects.requireNonNull(messages, "messages"));
    }

    /** The installed logger, or one that writes plain text through SLF4J when nothing is installed. */
    public static TerraLogger logger() {
        TerraLogger current = logger;
        if(current == null) {
            synchronized(TerraLog.class) {
                current = logger;
                if(current == null) {
                    current = new TerraLogger("TerraReforged",
                        component -> FALLBACK.info(MiniMessageTexts.plain(component)),
                        Messages.defaults());
                    logger = current;
                }
            }
        }
        return current;
    }
}
