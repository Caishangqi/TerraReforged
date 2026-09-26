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

package log;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dfsek.terra.banner.Banners;
import com.dfsek.terra.banner.PluginBanner;
import com.dfsek.terra.lang.Messages;
import com.dfsek.terra.log.LogLevel;
import com.dfsek.terra.log.TerraLogger;
import com.dfsek.terra.text.MiniMessageTexts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/// Covers the parts of M7 that need no server: message fallback, placeholder and prefix resolution,
/// colour, and the shape of the banner.
class StyledLoggingTest {
    private final List<Component> sent = new ArrayList<>();

    private TerraLogger logger(Messages messages) {
        return new TerraLogger("TerraReforged", sent::add, messages);
    }

    private String lastPlain() {
        return MiniMessageTexts.plain(sent.get(sent.size() - 1));
    }

    /// Every colour in the last line's tree. Asserting on a colour rather than on a tree position keeps
    /// the test from depending on how MiniMessage happens to nest its output.
    private Set<TextColor> lastColors() {
        Set<TextColor> colors = new LinkedHashSet<>();
        collectColors(sent.getLast(), colors);
        return colors;
    }

    private static void collectColors(Component component, Set<TextColor> into) {
        if(component.color() != null) {
            into.add(component.color());
        }
        component.children().forEach(child -> collectColors(child, into));
    }

    @Test
    void prefixCarriesThePluginNameAndTheKeyIsNotPrinted() {
        logger(Messages.defaults()).send("platform.ready");
        assertEquals("TerraReforged | TerraReforged finished initialization.", lastPlain());
    }

    @Test
    void placeholdersAreFilled() {
        logger(Messages.defaults()).send("platform.packs-loaded", "count", "3");
        assertEquals("TerraReforged | Loaded 3 config packs.", lastPlain());
    }

    @Test
    void hexColourInATemplateIsApplied() {
        logger(Messages.defaults()).send("platform.ready");
        assertTrue(lastColors().contains(TextColor.fromHexString("#55ffa4")), () -> "expected the success hex, got " + lastColors());
    }

    @Test
    void gradientInThePrefixIsResolved() {
        logger(Messages.defaults()).send("platform.ready");
        // A gradient is rendered as one colour per character, so the prefix alone contributes several.
        assertTrue(lastColors().size() > 3, () -> "expected a gradient across the prefix, got " + lastColors());
    }

    @Test
    void severityColoursAnUncolouredBody() {
        logger(Messages.defaults()).error("plain body");

        assertEquals("TerraReforged | plain body", lastPlain());
        assertTrue(lastColors().contains(TextColor.fromHexString("#e73f34")), () -> "expected the error hex, got " + lastColors());
    }

    @Test
    void aColouredBodyKeepsItsOwnColour() {
        logger(Messages.defaults()).log(LogLevel.ERROR, "<green>already coloured");

        assertEquals("TerraReforged | already coloured", lastPlain());
        assertTrue(lastColors().contains(NamedTextColor.GREEN), () -> "expected green to survive, got " + lastColors());
    }

    @Test
    void legacyCodesStillWork() {
        logger(Messages.defaults()).info("&alegacy");
        assertTrue(lastPlain().endsWith("legacy"));
    }

    @Test
    void anEditedFileOverridesTheDefaultAndABlankValueFallsBack(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(Messages.RESOURCE);
        Files.writeString(file, """
                                prefix: "<white>Reforged <dark_gray>> </dark_gray>"
                                platform:
                                  ready: "<prefix>edited line"
                                  packs-loading: ""
                                """, StandardCharsets.UTF_8);

        Messages messages = Messages.load(file, getClass().getClassLoader());
        TerraLogger logger = logger(messages);

        logger.send("platform.ready");
        assertEquals("Reforged > edited line", lastPlain());

        logger.send("platform.packs-loading");
        assertEquals("Reforged > Loading config packs...", lastPlain(), "a blank value falls back to the default text");
    }

    @Test
    void anAbsentFileIsWrittenFromTheJarAndLoads(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("nested").resolve(Messages.RESOURCE);

        Messages messages = Messages.load(file, getClass().getClassLoader());

        assertTrue(Files.isRegularFile(file), "the default lang.yml is dumped on first load");
        assertTrue(messages.has("platform.ready"));
        logger(messages).send("platform.ready");
        assertEquals("TerraReforged | TerraReforged finished initialization.", lastPlain());
    }

    @Test
    void theBannerPrintsTheLogoASpacerAndFourMetadataLines() {
        new PluginBanner(Banners.TERRA_REFORGED, logger(Messages.defaults()))
            .print(Map.of("version", "7.0.0", "authors", "dfsek, Caizii", "platform", "Purpur 26.2", "jvm", "25.0.2+12"));

        assertEquals(Banners.TERRA_REFORGED.length + 5, sent.size());
        assertEquals("", MiniMessageTexts.plain(sent.get(Banners.TERRA_REFORGED.length)), "a blank spacer follows the logo");

        List<String> metadata = sent.subList(Banners.TERRA_REFORGED.length + 1, sent.size())
            .stream()
            .map(MiniMessageTexts::plain)
            .toList();
        assertEquals(List.of(
            "TerraReforged v7.0.0",
            "Authors: dfsek, Caizii",
            "Running on Purpur 26.2",
            "JVM: 25.0.2+12"), metadata);
        assertFalse(metadata.stream().anyMatch(line -> line.contains("|")), "the banner carries no log prefix");
    }

    @Test
    void aFigletRowIsNotReadAsMarkup() {
        new PluginBanner(new String[]{ " | (-| | (_|| \\ (-| <not_a_tag>" }, logger(Messages.defaults()))
            .print(Map.of("version", "7.0.0", "authors", "Caizii", "platform", "Purpur 26.2", "jvm", "25"));

        assertEquals(" | (-| | (_|| \\ (-| <not_a_tag>", MiniMessageTexts.plain(sent.getFirst()));
    }
}
