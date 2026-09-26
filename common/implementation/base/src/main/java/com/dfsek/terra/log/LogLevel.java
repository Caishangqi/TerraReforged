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

/**
 * Console severity used by {@link TerraLogger}. The colour is applied only to a body that carries no
 * colour of its own, so a language template always wins over the severity.
 */
public enum LogLevel {
    INFO("#529ced"),
    SUCCESS("#55ffa4"),
    WARNING("#f9f178"),
    ERROR("#e73f34");

    private final String hexColor;

    LogLevel(String hexColor) {
        this.hexColor = hexColor;
    }

    public String hexColor() {
        return hexColor;
    }
}
