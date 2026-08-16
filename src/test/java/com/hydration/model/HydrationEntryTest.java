package com.hydration.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class HydrationEntryTest {

    @Test
    void recordStoresValues() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 14, 10, 0);
        HydrationEntry entry = new HydrationEntry(42, 300, now);

        assertEquals(42, entry.id());
        assertEquals(300, entry.amountMl());
        assertEquals(now, entry.timestamp());
    }

    @Test
    void ofFactoryUsesNowAndPlaceholderId() {
        HydrationEntry entry = HydrationEntry.of(500);

        assertEquals(-1, entry.id());
        assertEquals(500, entry.amountMl());

        // El timestamp debe ser "ahora" (dentro de una ventana de ±5 segundos).
        LocalDateTime before = LocalDateTime.now().minusSeconds(5);
        LocalDateTime after = LocalDateTime.now().plusSeconds(5);
        assertTrue(!entry.timestamp().isBefore(before) && !entry.timestamp().isAfter(after));
    }

    @Test
    void entriesWithDifferentIdsAreDifferent() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 14, 10, 0);
        assertNotEquals(new HydrationEntry(1, 300, now), new HydrationEntry(2, 300, now));
    }
}
