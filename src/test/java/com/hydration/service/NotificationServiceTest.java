package com.hydration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hydration.util.ToastManager;
import com.hydration.util.ToastManager.ToastAction;

class NotificationServiceTest {

    private ToastManager toastManager;
    private NotificationService notifier;

    @BeforeEach
    void setUp() {
        toastManager = mock(ToastManager.class);
        notifier = new NotificationService(toastManager, () -> {
        }, ml -> {
        });
    }

    @Test
    void sendReminder_belowGoal_showsToastWithThreeDrinkActions() {
        notifier.sendReminder(500, 2000);

        verify(toastManager).show(
                eq("Hydration Reminder"),
                eq("Llevas 500 ml de los 2000 ml del día. ¡Hora de tomar agua!"),
                eq("/icons/droplet.png"),
                any(Runnable.class),
                argThat(actions -> actions.stream()
                        .map(ToastAction::label)
                        .toList()
                        .equals(List.of("200 ml", "300 ml", "500 ml"))));
    }

    @Test
    void sendReminder_atGoal_showsToastWithoutActions() {
        notifier.sendReminder(2000, 2000);

        verify(toastManager).show(
                eq("Hydration Reminder"),
                eq("¡Meta cumplida! Llevas 2000 ml hoy. Un vaso más no viene mal"),
                eq("/icons/flame.png"),
                any(Runnable.class),
                eq(List.of()));
    }

    @Test
    void sendGoalReached_showsToastWithoutActions() {
        notifier.sendGoalReached(2000);

        verify(toastManager).show(
                eq("¡Meta del día cumplida!"),
                eq("Llegaste a los 2000 ml de hoy."),
                eq("/icons/flame.png"),
                any(Runnable.class),
                eq(List.of()));
    }
}
