package com.hydration.service;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.hydration.model.UserSettings;

/**
 * Programa los recordatorios periódicos. Corre sobre virtual threads para
 * no competir con el hilo de UI de JavaFX; toda interacción con nodos de
 * la UI debe hacerse vía Platform.runLater desde el consumidor de estos
 * eventos.
 */
public class ReminderScheduler {

    private final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(r -> Thread.ofVirtual().unstarted(r));

    private final DatabaseService db;
    private final NotificationService notifier;
    private final Supplier<UserSettings> settingsSupplier;

    private java.util.concurrent.ScheduledFuture<?> currentTask;
    private volatile boolean paused = false;

    public ReminderScheduler(DatabaseService db, NotificationService notifier,
            Supplier<UserSettings> settingsSupplier) {
        this.db = db;
        this.notifier = notifier;
        this.settingsSupplier = settingsSupplier;
    }

    /**
     * Arranca (o reinicia) el ciclo de recordatorios con el intervalo actual de
     * settings.
     */
    public void start() {
        stop();
        UserSettings settings = settingsSupplier.get();
        long intervalMinutes = settings.reminderIntervalMinutes();
        currentTask = executor.scheduleAtFixedRate(
                this::tick,
                intervalMinutes,
                intervalMinutes,
                TimeUnit.MINUTES);
    }

    public void stop() {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    private void tick() {
        if (paused)
            return;

        UserSettings settings = settingsSupplier.get();
        LocalTime now = LocalTime.now();

        if (settings.isWithinQuietHours(now))
            return;
        if (!settings.isWithinActiveHours(now))
            return;

        int currentMl = db.getTodayTotalMl();
        notifier.sendReminder(currentMl, settings.dailyGoalMl());
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
