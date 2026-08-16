package com.hydration.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class UserSettingsTest {

    private static UserSettings settings(LocalTime activeFrom, LocalTime activeTo,
            boolean quietEnabled, LocalTime quietFrom, LocalTime quietTo) {
        return new UserSettings(2000, 60, activeFrom, activeTo,
                quietEnabled, quietFrom, quietTo);
    }

    // ---- Horario activo: rango normal ----
    @Test
    void activeHours_normalRange_inclusiveBounds() {
        UserSettings s = settings(LocalTime.of(9, 0), LocalTime.of(18, 0),
                false, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertTrue(s.isWithinActiveHours(LocalTime.of(9, 0))); // borde inferior inclusivo
        assertTrue(s.isWithinActiveHours(LocalTime.of(12, 0))); // dentro
        assertTrue(s.isWithinActiveHours(LocalTime.of(18, 0))); // borde superior inclusivo
        assertFalse(s.isWithinActiveHours(LocalTime.of(8, 59)));
        assertFalse(s.isWithinActiveHours(LocalTime.of(18, 1)));
        assertFalse(s.isWithinActiveHours(LocalTime.of(0, 0)));
    }

    // ---- Horario activo: cruza la medianoche ----
    @Test
    void activeHours_midnightCrossing() {
        UserSettings s = settings(LocalTime.of(22, 0), LocalTime.of(7, 0),
                false, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertFalse(s.isWithinActiveHours(LocalTime.of(21, 0)));
        assertTrue(s.isWithinActiveHours(LocalTime.of(22, 0))); // borde
        assertTrue(s.isWithinActiveHours(LocalTime.of(23, 30)));
        assertTrue(s.isWithinActiveHours(LocalTime.of(0, 30))); // pasada la medianoche
        assertTrue(s.isWithinActiveHours(LocalTime.of(6, 59)));
        assertTrue(s.isWithinActiveHours(LocalTime.of(7, 0))); // borde
        assertFalse(s.isWithinActiveHours(LocalTime.of(7, 1)));
    }

    // ---- Horario activo: ventana de 24h (from == to) ----
    @Test
    void activeHours_equalStartEnd_isAllDay() {
        UserSettings s = settings(LocalTime.of(9, 0), LocalTime.of(9, 0),
                false, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertTrue(s.isWithinActiveHours(LocalTime.of(8, 0)));
        assertTrue(s.isWithinActiveHours(LocalTime.of(9, 0)));
        assertTrue(s.isWithinActiveHours(LocalTime.of(23, 59)));
    }

    // ---- Modo silencioso: deshabilitado ----
    @Test
    void quietHours_disabled_neverQuiet() {
        UserSettings s = settings(LocalTime.of(9, 0), LocalTime.of(18, 0),
                false, LocalTime.of(13, 0), LocalTime.of(15, 0));

        assertFalse(s.isWithinQuietHours(LocalTime.of(14, 0)));
        assertFalse(s.isWithinQuietHours(LocalTime.of(1, 0)));
    }

    // ---- Modo silencioso: rango normal ----
    @Test
    void quietHours_normalRange() {
        UserSettings s = settings(LocalTime.of(9, 0), LocalTime.of(18, 0),
                true, LocalTime.of(13, 0), LocalTime.of(15, 0));

        assertTrue(s.isWithinQuietHours(LocalTime.of(13, 0)));
        assertTrue(s.isWithinQuietHours(LocalTime.of(14, 0)));
        assertTrue(s.isWithinQuietHours(LocalTime.of(15, 0)));
        assertFalse(s.isWithinQuietHours(LocalTime.of(12, 59)));
        assertFalse(s.isWithinQuietHours(LocalTime.of(15, 1)));
    }

    // ---- Modo silencioso: cruza la medianoche ----
    @Test
    void quietHours_midnightCrossing() {
        UserSettings s = settings(LocalTime.of(9, 0), LocalTime.of(18, 0),
                true, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertFalse(s.isWithinQuietHours(LocalTime.of(21, 0)));
        assertTrue(s.isWithinQuietHours(LocalTime.of(23, 0)));
        assertTrue(s.isWithinQuietHours(LocalTime.of(1, 0)));
        assertTrue(s.isWithinQuietHours(LocalTime.of(6, 0)));
        assertFalse(s.isWithinQuietHours(LocalTime.of(8, 0)));
    }
}
