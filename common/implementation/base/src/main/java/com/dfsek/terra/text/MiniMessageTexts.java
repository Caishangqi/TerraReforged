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

package com.dfsek.terra.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Turns an operator-authored template into an Adventure {@link Component}.
 * <p>
 * Accepted input:
 * <ul>
 *     <li>MiniMessage tags such as {@code <gradient:#7bd85a:#00b7ff>} and hex {@code <#529ced>}</li>
 *     <li>Self-closing placeholder tags such as {@code <prefix>} and {@code <version>}</li>
 *     <li>Legacy ampersand codes such as {@code &a} and {@code &#RRGGBB}</li>
 *     <li>Section-sign codes such as {@code §a}</li>
 *     <li>Percent tokens such as {@code %pack%}</li>
 * </ul>
 * Hex conversion is done by the Adventure serializers. There is no hand-written colour parser here,
 * because the two would disagree on an edge case and only one of them would be the documented one.
 */
public final class MiniMessageTexts {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern PERCENT_TOKEN = Pattern.compile("%([a-z0-9_]+)%", Pattern.CASE_INSENSITIVE);

    /** MiniMessage tag names are restricted; a placeholder key outside this shape is skipped rather than throwing. */
    private static final Pattern TAG_NAME = Pattern.compile("[a-z0-9_-]+");

    private MiniMessageTexts() {
    }

    /** The shared instance, for callers that already hold MiniMessage input and need no placeholders. */
    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    /** Parses MiniMessage, {@code &} / {@code &#RRGGBB}, or {@code §} input. */
    public static Component deserialize(String input) {
        return deserialize(input, null, Map.of());
    }

    /** Parses a template after substituting {@code %key%} tokens and {@code <key>} tags from {@code values}. */
    public static Component deserialize(String template, Map<String, String> values) {
        return deserialize(template, null, values);
    }

    /**
     * Parses a template with a {@code <prefix>} tag plus named placeholders.
     *
     * @param template the operator-authored template
     * @param prefix   MiniMessage inserted by {@code <prefix>}; nullable, in which case the tag is left unresolved
     * @param values   {@code %key%} tokens and {@code <key>} tags. A value that itself looks like MiniMessage is parsed.
     */
    public static Component deserialize(String template, String prefix, Map<String, String> values) {
        Objects.requireNonNull(template, "template");
        Map<String, String> tags = values == null ? Map.of() : values;
        return deserializeResolved(applyTokens(template, tags), prefix, tags);
    }

    /** Replaces {@code %key%} tokens. An unknown key is left as written, so a typo is visible rather than blank. */
    public static String applyTokens(String template, Map<String, String> values) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(values, "values");
        Matcher matcher = PERCENT_TOKEN.matcher(template);
        StringBuilder output = new StringBuilder();
        while(matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement = values.getOrDefault(key, matcher.group());
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    /** Strips all formatting, for a line that has to go somewhere without colour support. */
    public static String plain(Component component) {
        return PLAIN.serialize(Objects.requireNonNull(component, "component"));
    }

    static boolean looksLikeMiniMessage(String input) {
        return input.indexOf('<') >= 0 && input.indexOf('>') >= 0;
    }

    private static Component deserializeResolved(String input, String prefix, Map<String, String> tags) {
        if(input.isEmpty()) {
            return Component.empty();
        }
        boolean hasPrefix = prefix != null && !prefix.isEmpty();
        if(looksLikeMiniMessage(input) || hasPrefix) {
            return miniMessageWithTags(prefix, tags).deserialize(input);
        }
        if(input.indexOf('§') >= 0) {
            return SECTION.deserialize(input);
        }
        return AMPERSAND.deserialize(input);
    }

    private static MiniMessage miniMessageWithTags(String prefix, Map<String, String> tags) {
        TagResolver.Builder builder = TagResolver.builder().resolver(TagResolver.standard());
        if(prefix != null && !prefix.isEmpty()) {
            builder.tag("prefix", Tag.selfClosingInserting(MINI_MESSAGE.deserialize(prefix)));
        }
        for(Map.Entry<String, String> entry : tags.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            if(!TAG_NAME.matcher(name).matches()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            Component inserted = looksLikeMiniMessage(value) ? MINI_MESSAGE.deserialize(value) : Component.text(value);
            builder.tag(name, Tag.selfClosingInserting(inserted));
        }
        return MiniMessage.builder().tags(builder.build()).build();
    }
}
