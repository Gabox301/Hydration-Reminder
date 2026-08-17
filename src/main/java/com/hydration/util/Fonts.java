package com.hydration.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.text.Font;

/**
 * Carga las fuentes del proyecto como instancias estáticas vía InputStream.
 *
 * <p>
 * {@code @font-face} en CSS falla cuando el classpath tiene espacios (el
 * loader de CSS usa {@code Font.loadFont(String)} con la URL codificada, que
 * no decodifica {@code %20}); cargar por stream lo evita.
 */
public final class Fonts {

    private static final String[] STATIC_FONTS = {
            "/fonts/static/Manrope-Regular.ttf",
            "/fonts/static/Manrope-SemiBold.ttf",
            "/fonts/static/Manrope-Bold.ttf",
            "/fonts/static/Manrope-ExtraBold.ttf",
            "/fonts/static/Fraunces-Regular.ttf",
            "/fonts/static/Fraunces-Medium.ttf",
    };

    private Fonts() {
    }

    public static void loadAll(Class<?> context) {
        for (String path : STATIC_FONTS) {
            byte[] fontBytes = readAllBytes(context, path);
            loadForJavaFx(path, fontBytes);
        }
    }

    private static void loadForJavaFx(String path, byte[] fontBytes) {
        try (InputStream in = new ByteArrayInputStream(fontBytes)) {
            if (Font.loadFont(in, 12) == null) {
                throw new IllegalStateException("JavaFX no pudo cargar la fuente: " + path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la fuente: " + path, e);
        }
    }

    private static byte[] readAllBytes(Class<?> context, String path) {
        try (InputStream in = context.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró la fuente: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la fuente: " + path, e);
        }
    }
}
