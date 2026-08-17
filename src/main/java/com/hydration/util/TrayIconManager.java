package com.hydration.util;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import javafx.application.Platform;

/**
 * Configura el ícono de la bandeja del sistema y su menú contextual usando
 * {@link java.awt.SystemTray} + {@link TrayIcon}.
 *
 * <p>
 * Se migró desde Dorkbox SystemTray porque esa librería (4.4) no expone un
 * callback de click izquierdo y, en Windows, su menú nativo falla con NPE en
 * algunos equipos (dorkbox/SystemTray #209), dejando la app sin forma de
 * reabrir la ventana ni salir. Con AWT se distingue click izquierdo (reabrir
 * la ventana principal) de click derecho (menú contextual, que el sistema
 * muestra automáticamente). Las notificaciones nativas siguen usando
 * {@code dorkbox.notify}, que es independiente del ícono de la bandeja.
 */
public class TrayIconManager {

    private final SystemTray systemTray;
    private final TrayIcon trayIcon;

    public TrayIconManager(
            InputStream iconStream,
            Runnable onOpenApp,
            Consumer<Integer> onQuickLog,
            Runnable onTogglePause,
            Runnable onExit) {
        this(SystemTray.getSystemTray(), iconStream, onOpenApp, onQuickLog, onTogglePause, onExit);
    }

    /**
     * Constructor de inyección (principalmente para tests): permite pasar una
     * instancia de {@link SystemTray} en lugar de resolverla vía
     * {@code SystemTray.getSystemTray()}.
     */
    public TrayIconManager(
            SystemTray systemTray,
            InputStream iconStream,
            Runnable onOpenApp,
            Consumer<Integer> onQuickLog,
            Runnable onTogglePause,
            Runnable onExit) {
        if (systemTray == null) {
            throw new RuntimeException("No se encontró soporte de bandeja del sistema en este entorno");
        }
        this.systemTray = systemTray;

        TrayIcon icon = new TrayIcon(
                loadImage(iconStream),
                "HydrationReminder",
                buildMenu(onOpenApp, onQuickLog, onTogglePause, onExit));
        icon.setImageAutoSize(true);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Platform.runLater(onOpenApp);
                }
            }
        });
        this.trayIcon = icon;

        try {
            systemTray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException("No se pudo agregar el ícono a la bandeja del sistema", e);
        }
    }

    private static java.awt.Image loadImage(InputStream iconStream) {
        try {
            BufferedImage image = ImageIO.read(iconStream);
            if (image == null) {
                throw new IOException("El stream del ícono no es una imagen legible");
            }
            return image;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el ícono de la bandeja", e);
        }
    }

    private static PopupMenu buildMenu(
            Runnable onOpenApp,
            Consumer<Integer> onQuickLog,
            Runnable onTogglePause,
            Runnable onExit) {
        PopupMenu menu = new PopupMenu();
        menu.add(item("Abrir", () -> Platform.runLater(onOpenApp)));
        menu.addSeparator();
        // Mismos tamaños que los botones de la vista principal (MainController).
        menu.add(item("Registrar vaso (200 ml)", () -> onQuickLog.accept(200)));
        menu.add(item("Registrar vaso (300 ml)", () -> onQuickLog.accept(300)));
        menu.add(item("Registrar vaso (500 ml)", () -> onQuickLog.accept(500)));
        menu.addSeparator();
        menu.add(item("Pausar / Reanudar recordatorios", () -> Platform.runLater(onTogglePause)));
        menu.addSeparator();
        menu.add(item("Salir", () -> Platform.runLater(onExit)));
        return menu;
    }

    private static MenuItem item(String label, Runnable action) {
        MenuItem menuItem = new MenuItem(label);
        menuItem.addActionListener(e -> action.run());
        return menuItem;
    }

    /** Quita el ícono de la bandeja (usado al cerrar la app). */
    public void remove() {
        systemTray.remove(trayIcon);
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }
}
