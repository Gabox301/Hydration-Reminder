package com.hydration.model;

import java.time.LocalTime;

/**
 * Configuración del usuario. Se persiste como fila única en SQLite
 * (o se puede migrar a un archivo de properties/JSON si se prefiere).
 */
public record UserSettings(
        int dailyGoalMl,
        int reminderIntervalMinutes,
        LocalTime activeFrom,
        LocalTime activeTo,
        boolean quietModeEnabled,
        LocalTime quietFrom,
        LocalTime quietTo) {
    public static UserSettings defaults() {
        return new UserSettings(
                2000,
                60,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                LocalTime.of(22, 0),
                LocalTime.of(7, 0));
    }

    /** Determina si, dado el momento actual, corresponde notificar. */
    public boolean isWithinActiveHours(LocalTime now) {
        return betweenInclusive(activeFrom, activeTo, now);
    }

    public boolean isWithinQuietHours(LocalTime now) {
        return quietModeEnabled && betweenInclusive(quietFrom, quietTo, now);
    }

    /**
     * Chequeo de rango horario inclusivo (from &lt;= now &lt;= to).
     * Soporta rangos que cruzan la medianoche (ej. 22:00 -&gt; 07:00) y, si
     * {@code from} y {@code to} coinciden, se interpreta como ventana de 24 h.
     */
    private static boolean betweenInclusive(LocalTime from, LocalTime to, LocalTime now) {
        if (from.isBefore(to)) {
            return !now.isBefore(from) && !now.isAfter(to);
        }
        // Rango que cruza la medianoche (ej. 22:00 -> 07:00): activo desde 'from'
        // hacia adelante o hasta 'to' hacia atrás.
        return !now.isBefore(from) || !now.isAfter(to);
    }
}
