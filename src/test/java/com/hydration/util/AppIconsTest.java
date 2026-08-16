package com.hydration.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.image.Image;

class AppIconsTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyRunning) {
            // El toolkit ya estaba en ejecución (otro test en este JVM).
        }
    }

    @Test
    void loadAll_returnsListOfImages() {
        List<Image> icons = AppIcons.loadAll(AppIconsTest.class);

        assertNotNull(icons);
        assertFalse(icons.isEmpty());
    }

    @Test
    void loadAll_allImagesAreNonNull() {
        List<Image> icons = AppIcons.loadAll(AppIconsTest.class);

        for (Image icon : icons) {
            assertNotNull(icon);
        }
    }

    @Test
    void loadAll_loadsMultipleSizes() {
        List<Image> icons = AppIcons.loadAll(AppIconsTest.class);

        // Esperamos al menos algunos de los tamaños definidos (16, 24, 32, 48, 64, 128,
        // 256, 512)
        assertTrue(icons.size() > 0, "Debería cargar al menos una imagen de ícono");
    }
}
