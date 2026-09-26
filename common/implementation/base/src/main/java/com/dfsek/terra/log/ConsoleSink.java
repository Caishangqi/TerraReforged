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

import net.kyori.adventure.text.Component;


/**
 * The platform's console, as one method. Bukkit passes its {@code ConsoleCommandSender}, which is an
 * Adventure {@code Audience}; a platform without one passes a sink that serializes to plain text.
 */
@FunctionalInterface
public interface ConsoleSink {
    /** Writes one fully rendered line to the platform console. */
    void send(Component component);
}
