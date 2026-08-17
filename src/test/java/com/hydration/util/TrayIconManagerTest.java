package com.hydration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrayIconManagerTest {

    // PNG válido de 1x1 (transparente): ImageIO necesita una imagen real.
    private static final String ONE_PX_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Mock
    private SystemTray mockSystemTray;

    private static InputStream iconStream() {
        return new ByteArrayInputStream(Base64.getDecoder().decode(ONE_PX_PNG_BASE64));
    }

    @Test
    void constructor_throwsWhenSystemTrayIsNull() {
        assertThrows(RuntimeException.class, () -> new TrayIconManager(
                (SystemTray) null,
                InputStream.nullInputStream(),
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
    void constructor_addsTrayIconToSystemTray() throws Exception {
        TrayIconManager manager = new TrayIconManager(
                mockSystemTray, iconStream(), () -> {
                }, ml -> {
                }, () -> {
                }, () -> {
                });

        verify(mockSystemTray).add(any(TrayIcon.class));
        assertNotNull(manager.getTrayIcon());
    }

    @Test
    void getTrayIcon_hasTooltipAndAutoSize() throws Exception {
        TrayIconManager manager = new TrayIconManager(
                mockSystemTray, iconStream(), () -> {
                }, ml -> {
                }, () -> {
                }, () -> {
                });

        assertEquals("Hydration Reminder", manager.getTrayIcon().getToolTip());
        assertEquals(true, manager.getTrayIcon().isImageAutoSize());
    }
}
