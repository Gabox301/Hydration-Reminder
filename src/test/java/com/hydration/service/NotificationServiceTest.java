package com.hydration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private NotificationService notifier;

    @BeforeEach
    void setUp() {
        notifier = new NotificationService();
    }

    @Test
    void sendReminder_belowGoal_doesNotThrow() {
        assertDoesNotThrow(() -> notifier.sendReminder(500, 2000));
    }

    @Test
    void sendReminder_atGoal_doesNotThrow() {
        assertDoesNotThrow(() -> notifier.sendReminder(2000, 2000));
    }

    @Test
    void sendReminder_aboveGoal_doesNotThrow() {
        assertDoesNotThrow(() -> notifier.sendReminder(2500, 2000));
    }

    @Test
    void sendGoalReached_doesNotThrow() {
        // sendGoalReached() dispara la notificación nativa; no debe lanzar.
        assertDoesNotThrow(() -> notifier.sendGoalReached(2000));
    }
}
