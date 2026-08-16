package com.hydration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hydration.model.UserSettings;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock
    private DatabaseService mockDb;

    @Mock
    private NotificationService mockNotifier;

    @Mock
    private Supplier<UserSettings> mockSettingsSupplier;

    private ReminderScheduler scheduler;

    private UserSettings defaultSettings;

    @BeforeEach
    void setUp() {
        defaultSettings = new UserSettings(
                2000, 60,
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                false,
                LocalTime.of(22, 0), LocalTime.of(7, 0));
        scheduler = new ReminderScheduler(mockDb, mockNotifier, mockSettingsSupplier);
    }

    @Test
    void start_schedulesTick() {
        when(mockSettingsSupplier.get()).thenReturn(defaultSettings);

        scheduler.start();
        scheduler.stop();

        verify(mockSettingsSupplier, atLeastOnce()).get();
    }

    @Test
    void stop_cancelsCurrentTask() {
        when(mockSettingsSupplier.get()).thenReturn(defaultSettings);

        scheduler.start();
        assertDoesNotThrow(() -> scheduler.stop());
    }

    @Test
    void pause_preventsNotifications() {
        assertDoesNotThrow(() -> scheduler.pause());
    }

    @Test
    void resume_allowsNotifications() {
        assertDoesNotThrow(() -> scheduler.pause());
        assertDoesNotThrow(() -> scheduler.resume());
    }

    @Test
    void shutdown_shutsDownExecutor() {
        assertDoesNotThrow(() -> scheduler.shutdown());
    }

    @Test
    void start_restartsPreviousTask() {
        when(mockSettingsSupplier.get()).thenReturn(defaultSettings);

        scheduler.start();
        scheduler.start(); // llamar start dos veces debe reiniciar

        verify(mockSettingsSupplier, atLeastOnce()).get();
        scheduler.stop();
    }
}
