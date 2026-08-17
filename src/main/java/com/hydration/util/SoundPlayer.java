package com.hydration.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Reproduce el sonido de notificación junto con la toast.
 *
 * <p>
 * JavaFX {@link Media} no puede cargar un recurso directamente desde el
 * classpath cuando vive dentro de un JAR (no admite URIs {@code jar:}), así
 * que el MP3 se extrae a un archivo temporal la primera vez y se reproduce con
 * {@link MediaPlayer}. Fallos de audio nunca deben romper la notificación: se
 * degradan con gracia.
 */
public final class SoundPlayer {

    private static final String RESOURCE = "/sounds/notification.mp3";

    private SoundPlayer() {
    }

    public static void playNotification() {
        try {
            Path tmp = Files.createTempFile("hydration-notification-", ".mp3");
            try (InputStream in = SoundPlayer.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    return;
                }
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            MediaPlayer player = new MediaPlayer(new Media(tmp.toUri().toString()));
            player.setOnEndOfMedia(() -> {
                player.dispose();
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    // mejor esfuerzo: el SO limpia el temporal por su cuenta
                }
            });
            player.play();
        } catch (IOException | RuntimeException e) {
            // degradación con gracia: la toast se muestra igual sin sonido
        }
    }
}
