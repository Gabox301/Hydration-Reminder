package com.hydration.util;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.text.Font;

/**
 * Carga las fuentes del proyecto como instancias estaticas via InputStream.
 *
 * <p>
 * {@code @font-face} en CSS falla cuando el classpath tiene espacios (el
 * loader de CSS usa {@code Font.loadFont(String)} con la URL codificada, que
 * no decodifica {@code %20}); cargar por stream lo evita.
 *
 * <p>
 * Además de registrarlas para JavaFX, cada fuente se registra también en el
 * {@link GraphicsEnvironment} de AWT. Es necesario porque
 * {@code dorkbox.notify.Notify} dibuja las notificaciones nativas con
 * Java2D/Swing (no con JavaFX), así que sin este registro no puede resolver
 * "Manrope" ni sus variantes por nombre y las notificaciones caerían en una
 * fuente por defecto del sistema en vez de la tipografía de marca (ver
 * {@link com.hydration.service.NotificationService}).
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
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (String path : STATIC_FONTS) {
            byte[] fontBytes = readAllBytes(context, path);

            loadForJavaFx(path, fontBytes);
            registerForAwt(ge, path, fontBytes);
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

    private static void registerForAwt(GraphicsEnvironment ge, String path, byte[] fontBytes) {
        try (InputStream in = new ByteArrayInputStream(fontBytes)) {
            java.awt.Font awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
            ge.registerFont(awtFont);
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la fuente: " + path, e);
        } catch (FontFormatException e) {
            throw new IllegalStateException("AWT no pudo interpretar la fuente: " + path, e);
        }
    }

    private static byte[] readAllBytes(Class<?> context, String path) {
        try (InputStream in = context.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("No se encontro la fuente: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la fuente: " + path, e);
        }
    }
}
