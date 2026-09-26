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

package com.dfsek.terra.lang;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * The console and operator message templates, keyed by a dotted path such as {@code platform.ready}.
 * <p>
 * The English defaults live in {@link #defaults()} rather than only in the shipped {@code lang.yml},
 * so a deleted, blank or malformed key falls back instead of printing its own name. That is why
 * {@link #load} merges the file over the defaults rather than replacing them.
 */
public final class Messages {
    /** The resource inside the jar, and the file name written into the data folder. */
    public static final String RESOURCE = "lang.yml";

    private final Map<String, String> lines;

    private Messages(Map<String, String> lines) {
        this.lines = Map.copyOf(lines);
    }

    public static Messages defaults() {
        Map<String, String> lines = new LinkedHashMap<>();

        // <name> is the plugin name from plugin.yml. Nothing here repeats the literal, so a rename is
        // one edit in one file.
        lines.put("prefix", "<gradient:#7bd85a:#00b7ff><name></gradient> <dark_gray>|</dark_gray> ");

        lines.put("banner.version", "<#529ced><name> <white>v<version>");
        lines.put("banner.authors", "<#529ced>Authors: <white><authors>");
        lines.put("banner.platform", "<#529ced>Running on <white><platform>");
        lines.put("banner.jvm", "<#529ced>JVM: <white><jvm>");

        lines.put("platform.server-version",
            "<prefix><#529ced>Running on Minecraft <white><minecraft></white> with server implementation <white><implementation></white>.");
        lines.put("platform.bukkit-server",
            "<prefix><#e73f34>This is a CraftBukkit or Bukkit server. <name> needs Paper; please upgrade.");
        lines.put("platform.spigot-server",
            "<prefix><#e73f34>This is a Spigot server. <name> needs Paper; please upgrade.");
        lines.put("platform.command-failure",
            "<prefix><#e73f34><name> has been disabled: its commands could not be registered. Please report this.");

        lines.put("platform.initializing", "<prefix><#529ced>Initializing <name>...");
        lines.put("platform.config-loading", "<prefix><#529ced>Loading <white>config.yml</white>.");
        lines.put("platform.config-dumping", "<prefix><#529ced>Writing the default <white>config.yml</white>.");
        lines.put("platform.config-missing", "<prefix><#f9f178>config.yml is not present in the jar.");
        lines.put("platform.lang-dumping", "<prefix><#529ced>Writing the default <white><file></white>.");
        lines.put("platform.lang-failed", "<prefix><#f9f178>Could not read <white><file></white>; using the built-in messages.");
        lines.put("platform.addons-loaded", "<prefix><#55ffa4>Loaded <white><count></white> addons.");
        lines.put("platform.ready", "<prefix><#55ffa4><name> finished initialization.");

        lines.put("platform.packs-loading", "<prefix><#529ced>Loading config packs...");
        lines.put("platform.packs-loaded", "<prefix><#55ffa4>Loaded <white><count></white> config packs.");
        lines.put("platform.packs-partial",
            "<prefix><#f9f178>Loaded <white><count></white> config packs; <white><failed></white> could not be read. The log holds each reason.");
        lines.put("platform.packs-failed", "<prefix><#e73f34>Failed to load config packs. The log holds the reason.");
        lines.put("platform.metapacks-loading", "<prefix><#529ced>Loading meta config packs...");
        lines.put("platform.metapacks-loaded", "<prefix><#55ffa4>Loaded <white><count></white> meta config packs.");
        lines.put("platform.metapacks-partial",
            "<prefix><#f9f178>Loaded <white><count></white> meta config packs; <white><failed></white> could not be read. The log holds each reason.");
        lines.put("platform.metapacks-failed", "<prefix><#e73f34>Failed to load meta config packs. The log holds the reason.");

        lines.put("platform.generator-replaced",
            "<prefix><#529ced>Replaced the pack in the chunk generator for world <white><world></white>.");

        return new Messages(lines);
    }

    /**
     * Reads {@code file}, writing the jar's default there first when it is absent, and merges what it
     * finds over {@link #defaults()}. Only keys the defaults know are taken, so an operator cannot
     * introduce a key that nothing reads and believe it does something.
     *
     * @param file   the message file in the data folder
     * @param loader the class loader holding {@link #RESOURCE}
     */
    public static Messages load(Path file, ClassLoader loader) throws IOException {
        Messages base = defaults();
        if(!Files.isRegularFile(file)) {
            copyDefault(loader, file);
        }
        if(!Files.isRegularFile(file)) {
            return base;
        }

        Map<String, String> flat = new LinkedHashMap<>();
        try(InputStream stream = Files.newInputStream(file)) {
            flatten("", new Yaml().load(stream), flat);
        } catch(RuntimeException e) {
            // A malformed file must not stop a server from starting, and the caller reports it.
            throw new IOException("Malformed " + file, e);
        }

        Map<String, String> merged = new LinkedHashMap<>(base.lines);
        base.lines.keySet().forEach(key -> {
            String value = flat.get(key);
            if(value != null && !value.isBlank()) {
                merged.put(key, value);
            }
        });
        return new Messages(merged);
    }

    /** Writes {@link #RESOURCE} to {@code file}, creating parent directories. A present file is kept. */
    public static void copyDefault(ClassLoader loader, Path file) throws IOException {
        try(InputStream stream = loader.getResourceAsStream(RESOURCE)) {
            if(stream == null) {
                return;
            }
            Path parent = file.getParent();
            if(parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(stream, file);
        }
    }

    /** The {@code <prefix>} template, before {@code <name>} is resolved. */
    public String prefix() {
        return lines.getOrDefault("prefix", "");
    }

    /** The template for {@code key}, or the key itself when the defaults do not define it. */
    public String template(String key) {
        return lines.getOrDefault(key, key);
    }

    public boolean has(String key) {
        return lines.containsKey(key);
    }

    /** Flattens nested YAML maps to dotted keys. Lists are skipped: no template is a list. */
    private static void flatten(String prefix, Object raw, Map<String, String> out) {
        if(!(raw instanceof Map<?, ?> map)) {
            return;
        }
        for(Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if(value instanceof Map<?, ?>) {
                flatten(key, value, out);
            } else if(value != null && !(value instanceof Iterable<?>)) {
                out.put(key, String.valueOf(value));
            }
        }
    }
}
