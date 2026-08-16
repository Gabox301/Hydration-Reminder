package com.hydration.service;

import dorkbox.systemTray.SystemTray;

/**
 * Envía notificaciones nativas del sistema operativo a través del tray.
 * Se apoya en la instancia de SystemTray creada por TrayIconManager.
 */
public class NotificationService {

    private final SystemTray systemTray;

    public NotificationService(SystemTray systemTray) {
        this.systemTray = systemTray;
    }

    public void sendReminder(int currentMl, int goalMl) {
        String message = currentMl >= goalMl
                ? "¡Meta cumplida! Llevás %d ml hoy. Un vaso más no viene mal 💧".formatted(currentMl)
                : "Hora de tomar agua 💧 (%d / %d ml hoy)".formatted(currentMl, goalMl);

        systemTray.getMenu(); // asegura que el tray esté inicializado
        dorkbox.notify.Notify.Companion.create()
                .title("HydrationReminder")
                .text(message)
                .hideAfter(8000)
                .show();
    }

    public void sendGoalReached(int goalMl) {
        dorkbox.notify.Notify.Companion.create()
                .title("¡Meta del día cumplida! 🎉")
                .text("Llegaste a los %d ml de hoy.".formatted(goalMl))
                .hideAfter(8000)
                .show();
    }
}
