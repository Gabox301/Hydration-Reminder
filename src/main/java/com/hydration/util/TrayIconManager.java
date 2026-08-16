package com.hydration.util;

import java.util.function.Consumer;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;

/**
 * Configura el ícono de la bandeja del sistema y su menú contextual.
 * Usa Dorkbox SystemTray por su mejor soporte cross-platform frente a
 * java.awt.SystemTray puro.
 */
public class TrayIconManager {

    private final SystemTray systemTray;

    public TrayIconManager(
            String iconPath,
            Runnable onOpenApp,
            Consumer<Integer> onQuickLog,
            Runnable onTogglePause,
            Runnable onExit) {
        this(SystemTray.get(), iconPath, onOpenApp, onQuickLog, onTogglePause, onExit);
    }

    /**
     * Constructor de inyección (principalmente para tests): permite pasar una
     * instancia de {@link SystemTray} en lugar de resolverla vía
     * {@code SystemTray.get()}.
     */
    public TrayIconManager(
            SystemTray systemTray,
            String iconPath,
            Runnable onOpenApp,
            Consumer<Integer> onQuickLog,
            Runnable onTogglePause,
            Runnable onExit) {
        this.systemTray = systemTray;
        if (systemTray == null) {
            throw new RuntimeException("No se encontró soporte de bandeja del sistema en este entorno");
        }
        systemTray.setImage(iconPath);
        systemTray.setTooltip("HydrationReminder");

        systemTray.getMenu().add(new MenuItem("Abrir", e -> Platform.runLater(onOpenApp)));

        // Mismos tamaños que los botones de la vista principal (MainController).
        systemTray.getMenu().add(new MenuItem("Registrar vaso (200 ml)", e -> onQuickLog.accept(200)));
        systemTray.getMenu().add(new MenuItem("Registrar vaso (300 ml)", e -> onQuickLog.accept(300)));
        systemTray.getMenu().add(new MenuItem("Registrar vaso (500 ml)", e -> onQuickLog.accept(500)));

        systemTray.getMenu().add(new MenuItem("Pausar / Reanudar recordatorios", e -> onTogglePause.run()));

        systemTray.getMenu().add(new MenuItem("Salir", e -> {
            onExit.run();
            systemTray.shutdown();
            Platform.exit();
        }));
    }

    public SystemTray getSystemTray() {
        return systemTray;
    }
}
