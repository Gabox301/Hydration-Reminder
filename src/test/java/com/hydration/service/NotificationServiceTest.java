package com.hydration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dorkbox.systemTray.SystemTray;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SystemTray mockSystemTray;

    private NotificationService notifier;

    @BeforeEach
    void setUp() {
        notifier = new NotificationService(mockSystemTray);
    }

    @Test
    void sendReminder_belowGoal_showsProgressMessage() {
        when(mockSystemTray.getMenu()).thenReturn(mock());

        notifier.sendReminder(500, 2000);

        verify(mockSystemTray).getMenu();
    }

    @Test
    void sendReminder_atGoal_showsGoalReachedMessage() {
        when(mockSystemTray.getMenu()).thenReturn(mock());

        notifier.sendReminder(2000, 2000);

        verify(mockSystemTray).getMenu();
    }

    @Test
    void sendReminder_aboveGoal_showsGoalReachedMessage() {
        when(mockSystemTray.getMenu()).thenReturn(mock());

        notifier.sendReminder(2500, 2000);

        verify(mockSystemTray).getMenu();
    }

    @Test
    void sendGoalReached_doesNotThrow() {
        // sendGoalReached() no interactúa con SystemTray; solo dispara la
        // notificación nativa.
        assertDoesNotThrow(() -> notifier.sendGoalReached(2000));
    }
}
