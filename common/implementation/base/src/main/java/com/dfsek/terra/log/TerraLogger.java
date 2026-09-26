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
import net.kyori.adventure.text.format.TextColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.dfsek.terra.lang.Messages;
import com.dfsek.terra.text.MiniMessageTexts;


/**
 * Writes {@code PluginName | Message} lines to the platform console.
 * <p>
 * A line comes from a {@link Messages} template, so its wording and colour belong to the operator.
 * {@code <name>} is always available to a template; anything else a call site wants is passed as a
 * placeholder. This does not replace SLF4J: a failure carrying a stack trace stays there, because a
 * trace is not a prefixed line and sending it twice is worse than either form alone.
 */
public final class TerraLogger {
    private final String pluginName;
    private final ConsoleSink sink;
    private final Messages messages;

    /** The rendered prefix, with {@code <name>} already resolved. Built once; every line appends to it. */
    private final String prefix;

    public TerraLogger(String pluginName, ConsoleSink sink, Messages messages) {
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.prefix = messages.prefix().replace("<name>", pluginName);
    }

    public String pluginName() {
        return pluginName;
    }

    public Messages messages() {
        return messages;
    }

    /** Sends the template at {@code key} with no placeholders beyond {@code <name>}. */
    public void send(String key) {
        send(key, Map.of());
    }

    /** Sends the template at {@code key}, resolving {@code <prefix>}, {@code <name>} and {@code values}. */
    public void send(String key, Map<String, String> values) {
        Objects.requireNonNull(key, "key");
        sink.send(render(messages.template(key), values));
    }

    /** Sends one key-value placeholder without building a map at the call site. */
    public void send(String key, String placeholder, String value) {
        send(key, Map.of(placeholder, value));
    }

    /** Sends an already-rendered component untouched. The banner uses this for its logo rows. */
    public void component(Component component) {
        sink.send(Objects.requireNonNull(component, "component"));
    }

    /**
     * Sends a plain or MiniMessage body under the severity colour, prefixed. For a line that has no
     * language key because its text comes from somewhere else, such as another plugin's report.
     */
    public void log(LogLevel level, String body) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(body, "body");
        Component colored = MiniMessageTexts.deserialize(body)
            .colorIfAbsent(TextColor.fromHexString(level.hexColor()));
        sink.send(MiniMessageTexts.deserialize(prefix).append(colored));
    }

    public void info(String body) {
        log(LogLevel.INFO, body);
    }

    public void success(String body) {
        log(LogLevel.SUCCESS, body);
    }

    public void warning(String body) {
        log(LogLevel.WARNING, body);
    }

    public void error(String body) {
        log(LogLevel.ERROR, body);
    }

    private Component render(String template, Map<String, String> values) {
        Map<String, String> tags = new LinkedHashMap<>(values == null ? Map.of() : values);
        tags.putIfAbsent("name", pluginName);
        return MiniMessageTexts.deserialize(template, prefix, tags);
    }
}
