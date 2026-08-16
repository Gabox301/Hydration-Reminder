package com.hydration.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

/**
 * Carga el set de íconos de la app (múltiples resoluciones) para asignar a
 * cualquier Stage vía
 * {@code stage.getIcons().addAll(AppIcons.loadAll(getClass()))}.
 * JavaFX elige automáticamente la resolución más apropiada según el contexto
 * (barra de título, taskbar, alt-tab, dock).
 */
public final class AppIcons {

    private static final int[] SIZES = { 16, 24, 32, 48, 64, 128, 256, 512 };

    private AppIcons() {
    }

    public static List<Image> loadAll(Class<?> resourceRoot) {
        List<Image> icons = new ArrayList<>();
        for (int size : SIZES) {
            String path = "/icons/app-icon-" + size + ".png";
            var url = resourceRoot.getResource(path);
            if (url != null) {
                icons.add(new Image(url.toExternalForm()));
            }
        }
        return icons;
    }
}
