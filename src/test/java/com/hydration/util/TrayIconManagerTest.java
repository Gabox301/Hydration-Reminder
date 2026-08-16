package com.hydration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dorkbox.systemTray.Menu;
import dorkbox.systemTray.SystemTray;

@ExtendWith(MockitoExtension.class)
class TrayIconManagerTest {

    @Mock
    private SystemTray mockSystemTray;

    @Mock
    private Menu mockMenu;

    @Test
    void constructor_throwsWhenSystemTrayIsNull() {
        assertThrows(RuntimeException.class, () -> new TrayIconManager(
                (SystemTray) null,
                "/icons/tray-icon.png",
                () -> {
                },
                ml -> {
                },
                () -> {
                },
                () -> {
                }));
    }

    @Test
    void constructor_setsImageAndTooltip() {
        when(mockSystemTray.getMenu()).thenReturn(mockMenu);

        new TrayIconManager(mockSystemTray, "/icons/tray-icon.png", () -> {
        }, ml -> {
        }, () -> {
        }, () -> {
        });

        verify(mockSystemTray).setImage("/icons/tray-icon.png");
        verify(mockSystemTray).setTooltip("HydrationReminder");
    }

    @Test
    void getSystemTray_returnsProvidedInstance() {
        when(mockSystemTray.getMenu()).thenReturn(mockMenu);

        TrayIconManager manager = new TrayIconManager(mockSystemTray, "/icons/tray-icon.png", () -> {
        }, ml -> {
        }, () -> {
        }, () -> {
        });

        assertEquals(mockSystemTray, manager.getSystemTray());
    }
}
