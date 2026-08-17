package com.hydration.service;

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import dorkbox.notify.Notify;
import dorkbox.notify.Position;
import dorkbox.notify.Theme;

/**
 * Envía notificaciones nativas del sistema operativo.
 * Usa {@code dorkbox.notify}, que es independiente del ícono de la bandeja.
 *
 * <p>
 * El tema visual replica la dirección "Marea nocturna" de {@code main.css}:
 * fondo tinta-petróleo, texto foam/cian, ícono de gota y tipografía Manrope,
 * en vez del cuadro blanco con texto simple que trae dorkbox.notify por
 * defecto. La librería dibuja el panel como un rectángulo sólido vía Java2D
 * (no soporta esquinas redondeadas, degradés ni el efecto "vidrio
 * esmerilado" de las tarjetas de la UI), así que paleta + tipografía + ícono
 * de marca es el máximo nivel de consistencia visual alcanzable acá.
 */
public class NotificationService {

    // Paleta "Marea nocturna" (mismos valores que las variables -fx-* de main.css).
    private static final Color INK_900 = new Color(0x0A, 0x1E, 0x28);
    private static final Color TIDE_300 = new Color(0x86, 0xE8, 0xDC);
    private static final Color TIDE_400 = new Color(0x4F, 0xD6, 0xC7);
    private static final Color FOAM = new Color(0xEA, 0xF7, 0xF5);
    private static final Color MIST = new Color(0x9F, 0xB8, 0xBD);

    /**
     * dorkbox.notify.Theme recibe las fuentes como texto "familia ESTILO
     * tamaño" (ver {@code FontUtil.parseFont} en dorkbox:Utilities): la
     * palabra pegada al tamaño siempre se descarta del nombre de familia, y
     * solo dispara negrita/cursiva sintética si es exactamente "BOLD" o
     * "ITALIC". Para conservar el nombre de familia de dos palabras
     * "Manrope ExtraBold" (ya es un peso extra bold real, no hace falta
     * sintetizarlo) se agrega "PLAIN" como relleno inerte antes del tamaño;
     * de lo contrario la librería recorta "ExtraBold" y se queda solo con
     * "Manrope".
     */
    private static final Theme MAREA_NOCTURNA_THEME = new Theme(
            "Manrope ExtraBold PLAIN 14",
            "Manrope PLAIN 12",
            INK_900,
            TIDE_300,
            FOAM,
            MIST,
            TIDE_400);

    private static final Image DROPLET_ICON = loadIcon("/icons/droplet.png");
    private static final Image FLAME_ICON = loadIcon("/icons/flame.png");

    public void sendReminder(int currentMl, int goalMl) {
        boolean goalMet = currentMl >= goalMl;
        String message = goalMet
                ? "¡Meta cumplida! Llevás %d ml hoy. Un vaso más no viene mal".formatted(currentMl)
                : "Hora de tomar agua (%d / %d ml hoy)".formatted(currentMl, goalMl);

        themedNotify(goalMet ? FLAME_ICON : DROPLET_ICON)
                .title("HydrationReminder")
                .text(message)
                .hideAfter(8000)
                .show();
    }

    public void sendGoalReached(int goalMl) {
        themedNotify(FLAME_ICON)
                .title("¡Meta del día cumplida!")
                .text("Llegaste a los %d ml de hoy.".formatted(goalMl))
                .hideAfter(8000)
                .show();
    }

    /**
     * Notificación base con el tema y el ícono de marca ya aplicados.
     * Importante: hay que seguir encadenando hacia {@code .show()} (nunca
     * {@code .showWarning()}/{@code .showInformation()}/etc.), porque esos
     * atajos pisan el ícono con el dibujo genérico de warning/info/error de
     * la librería antes de mostrar la notificación.
     */
    private static Notify themedNotify(Image icon) {
        Notify notify = Notify.Companion.create()
                .theme(MAREA_NOCTURNA_THEME)
                .position(Position.BOTTOM_RIGHT);
        return icon != null ? notify.image(icon) : notify;
    }

    private static Image loadIcon(String classpathPath) {
        try (InputStream in = NotificationService.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                return null; // degrada con gracia: la notificación se muestra sin ícono
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar el ícono " + classpathPath, e);
        }

    }
}
