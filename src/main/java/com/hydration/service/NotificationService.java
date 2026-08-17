package com.hydration.service;

import java.util.List;
import java.util.function.Consumer;

import com.hydration.util.ToastManager;
import com.hydration.util.ToastManager.ToastAction;

/**
 * Dispara las notificaciones de recordatorio como toasts propios de JavaFX
 * (ver {@link ToastManager}) con la estética "Marea nocturna" de la app.
 *
 * <p>
 * Es una fachada: construye el contenido (título, mensaje, ícono y acciones)
 * y delega el render en {@link ToastManager}, que crea una ventana sin
 * decorar con esquinas redondeadas, animación de entrada y auto-cierre. Los
 * botones de acción registran un vaso directo (200/300/500 ml, los mismos
 * tamaños que el menú de la bandeja) sin abrir la ventana principal; hacer
 * clic en el cuerpo de la toast abre la app.
 */
public class NotificationService {

    private final ToastManager toastManager;
    private final Runnable onOpenApp;
    private final Consumer<Integer> onQuickLog;

    public NotificationService(Runnable onOpenApp, Consumer<Integer> onQuickLog) {
        this(new ToastManager(), onOpenApp, onQuickLog);
    }

    NotificationService(ToastManager toastManager, Runnable onOpenApp,
            Consumer<Integer> onQuickLog) {
        this.toastManager = toastManager;
        this.onOpenApp = onOpenApp;
        this.onQuickLog = onQuickLog;
    }

    public void sendReminder(int currentMl, int goalMl) {
        boolean goalMet = currentMl >= goalMl;
        String message = goalMet
                ? "¡Meta cumplida! Llevas %d ml hoy. Un vaso más no viene mal".formatted(currentMl)
                : "Llevas %d ml de los %d ml del día. ¡Hora de tomar agua!"
                        .formatted(currentMl, goalMl);
        List<ToastAction> actions = goalMet
                ? List.of()
                : List.of(
                        new ToastAction("200 ml", () -> onQuickLog.accept(200)),
                        new ToastAction("300 ml", () -> onQuickLog.accept(300)),
                        new ToastAction("500 ml", () -> onQuickLog.accept(500)));
        toastManager.show(
                "Hydration Reminder",
                message,
                goalMet ? "/icons/flame.png" : "/icons/droplet.png",
                onOpenApp,
                actions);
    }

    public void sendGoalReached(int goalMl) {
        toastManager.show(
                "¡Meta del día cumplida!",
                "Llegaste a los %d ml de hoy.".formatted(goalMl),
                "/icons/flame.png",
                onOpenApp,
                List.of());
    }
}
