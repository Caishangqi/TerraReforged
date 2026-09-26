package com.dfsek.terra.bukkit.nms.v26_2;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import com.dfsek.terra.bukkit.NMSInitializer;
import com.dfsek.terra.bukkit.NMSInitializer.Binding;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * The version SPI's only seam is a class name that {@code NMSInitializer} builds and resolves at
 * runtime. Nothing in a compile crosses it: common cannot see this module, and this module never
 * mentions the name common computes. So a renamed package, a moved entry point, or a binding that
 * advertises a version no module serves all build cleanly and fail when a server starts.
 *
 * <p>These are the checks that fail instead.
 */
class NMSBindingTest {
    private static final String MODULE = "v26_2";

    private static Binding binding() {
        return NMSInitializer.BINDINGS.stream()
            .filter(candidate -> candidate.module().equals(MODULE))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No entry in NMSInitializer.BINDINGS declares the " + MODULE + " module."));
    }

    @Test
    void theBindingNamesThisModulesEntryPoint() {
        assertEquals(NMSPlatform.class.getName(), binding().platformClassName(),
            "NMSInitializer would load a class this module does not ship.");
    }

    @Test
    void everyAdvertisedVersionSelectsThisModule() {
        for(String version : binding().minecraftVersions()) {
            assertEquals(Optional.of(binding()), NMSInitializer.bindingFor(version),
                "Version " + version + " is advertised by this module but selects a different one.");
        }
    }

    /**
     * The binding table is Java and the Dev Bundle pin is Kotlin in {@code Versions.kt}; nothing else
     * connects them. Without this, bumping the Dev Bundle compiles the adapter against a new Minecraft
     * version while it still advertises the old one, and the plugin disables itself on a server it was
     * just built for. The build passes the pin in as a system property.
     */
    @Test
    void thePinnedMinecraftVersionSelectsThisModule() {
        // MinecraftVersionInfo renders a leading v and drops an absent patch segment, so the pin
        // "26.2" is the string "v26.2" at runtime.
        String pinned = "v" + System.getProperty("terra.test.minecraftVersion");

        assertEquals(Optional.of(binding()), NMSInitializer.bindingFor(pinned),
            "Versions.kt pins Minecraft " + pinned + ", which no binding in this module serves.");
    }
}
