package com.hydration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hydration.model.HydrationEntry;
import com.hydration.model.UserSettings;

class DatabaseServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService db;

    @BeforeEach
    void setUp() {
        // Cada test usa su propia DB temporal (SQLite con archivo real, no :memory:,
        // porque cada operación abre una conexión nueva).
        db = new DatabaseService(tempDir.resolve("test-" + System.nanoTime() + ".db"));
    }

    @Test
    void addEntry_incrementsTodayTotal() {
        assertEquals(0, db.getTodayTotalMl());

        db.addEntry(HydrationEntry.of(200));
        db.addEntry(HydrationEntry.of(300));

        assertEquals(500, db.getTodayTotalMl());
    }

    @Test
    void dailyTotals_includesToday() {
        db.addEntry(HydrationEntry.of(250));

        List<DatabaseService.DailyTotal> totals = db.getDailyTotals(14);

        assertFalse(totals.isEmpty());
        assertTrue(totals.stream().anyMatch(t -> t.totalMl() == 250));
    }

    @Test
    void loadSettings_returnsDefaultsWhenEmpty() {
        UserSettings settings = db.loadSettings();

        assertEquals(2000, settings.dailyGoalMl());
        assertEquals(60, settings.reminderIntervalMinutes());
        assertEquals(LocalTime.of(9, 0), settings.activeFrom());
        assertEquals(LocalTime.of(18, 0), settings.activeTo());
        assertFalse(settings.quietModeEnabled());
    }

    @Test
    void saveSettings_roundtripsValues() {
        UserSettings saved = new UserSettings(
                2500, 90,
                LocalTime.of(22, 0), LocalTime.of(7, 0),
                true,
                LocalTime.of(23, 0), LocalTime.of(6, 0));

        db.saveSettings(saved);
        UserSettings loaded = db.loadSettings();

        assertEquals(saved, loaded);
    }

    @Test
    void streak_isZeroWithoutHistory() {
        assertEquals(0, db.getCurrentStreakDays(2000));
    }
}
