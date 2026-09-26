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

/** Straight FIGlet banners. Rendered from the Straight font for the text of the plugin name. */
public final class Banners {
    /**
     * Straight FIGlet banner for TerraReforged.
     * <p>
     * Reference: <a href="https://patorjk.com/software/taag/#p=display&amp;f=Straight&amp;t=TerraReforged">
     * TAAG, font Straight</a>. The rows are escaped before rendering, so a backslash or an angle
     * bracket here is never read as MiniMessage.
     */
    public static final String[] TERRA_REFORGED = {
        "___          __    _",
        " |  _ _ _ _ |__) _(_ _  _ _  _ _|",
        " | (-| | (_|| \\ (-| (_)| (_)(-(_|",
        "                         _/"
    };

    private Banners() {
    }
}
