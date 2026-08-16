package com.hydration.service;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.hydration.model.HydrationEntry;
import com.hydration.model.UserSettings;

/**
 * Acceso a SQLite. Toda operación bloqueante debería llamarse desde un
 * virtual thread (ver ReminderScheduler) para no trabar la UI.
 */
public class DatabaseService {

    private final String jdbcUrl;

    public DatabaseService() {
        this(Path.of(System.getProperty("user.home"), ".hydration-reminder", "data.db"));
    }

    /**
     * Constructor para pruebas: permite apuntar a un archivo SQLite temporal en
     * lugar de la DB real del usuario. Es package-private deliberadamente.
     */
    DatabaseService(Path dbPath) {
        dbPath.getParent().toFile().mkdirs();
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        init();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void init() {
        String entries = """
                CREATE TABLE IF NOT EXISTS entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    amount_ml INTEGER NOT NULL,
                    timestamp TEXT NOT NULL
                )
                """;
        String settings = """
                CREATE TABLE IF NOT EXISTS settings (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    daily_goal_ml INTEGER NOT NULL,
                    reminder_interval_minutes INTEGER NOT NULL,
                    active_from TEXT NOT NULL,
                    active_to TEXT NOT NULL,
                    quiet_enabled INTEGER NOT NULL,
                    quiet_from TEXT NOT NULL,
                    quiet_to TEXT NOT NULL
                )
                """;
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(entries);
            st.execute(settings);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    public void addEntry(HydrationEntry entry) {
        String sql = "INSERT INTO entries (amount_ml, timestamp) VALUES (?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, entry.amountMl());
            ps.setString(2, entry.timestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar el registro", e);
        }
    }

    public int getTodayTotalMl() {
        String sql = "SELECT COALESCE(SUM(amount_ml), 0) FROM entries WHERE date(timestamp) = date('now', 'localtime')";
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo calcular el total del día", e);
        }
    }

    /** Totales por día, para el gráfico semanal/mensual (feature futura). */
    public List<DailyTotal> getDailyTotals(int lastNDays) {
        String sql = """
                SELECT date(timestamp) as day, SUM(amount_ml) as total
                FROM entries
                WHERE date(timestamp) >= date('now', ?)
                GROUP BY day
                ORDER BY day
                """;
        List<DailyTotal> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "-" + lastNDays + " days");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new DailyTotal(LocalDate.parse(rs.getString("day")), rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo obtener el historial", e);
        }
        return result;
    }

    public UserSettings loadSettings() {
        String sql = "SELECT * FROM settings WHERE id = 1";
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                UserSettings defaults = UserSettings.defaults();
                saveSettings(defaults);
                return defaults;
            }
            return new UserSettings(
                    rs.getInt("daily_goal_ml"),
                    rs.getInt("reminder_interval_minutes"),
                    LocalTime.parse(rs.getString("active_from")),
                    LocalTime.parse(rs.getString("active_to")),
                    rs.getInt("quiet_enabled") == 1,
                    LocalTime.parse(rs.getString("quiet_from")),
                    LocalTime.parse(rs.getString("quiet_to")));
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo cargar la configuración", e);
        }
    }

    public void saveSettings(UserSettings s) {
        String sql = """
                INSERT INTO settings (id, daily_goal_ml, reminder_interval_minutes, active_from, active_to, quiet_enabled, quiet_from, quiet_to)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    daily_goal_ml = excluded.daily_goal_ml,
                    reminder_interval_minutes = excluded.reminder_interval_minutes,
                    active_from = excluded.active_from,
                    active_to = excluded.active_to,
                    quiet_enabled = excluded.quiet_enabled,
                    quiet_from = excluded.quiet_from,
                    quiet_to = excluded.quiet_to
                """;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.dailyGoalMl());
            ps.setInt(2, s.reminderIntervalMinutes());
            ps.setString(3, s.activeFrom().toString());
            ps.setString(4, s.activeTo().toString());
            ps.setInt(5, s.quietModeEnabled() ? 1 : 0);
            ps.setString(6, s.quietFrom().toString());
            ps.setString(7, s.quietTo().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar la configuración", e);
        }
    }

    /** Días consecutivos (incluyendo hoy) donde el total alcanzó la meta diaria. */
    public int getCurrentStreakDays(int goalMl) {
        List<DailyTotal> totals = getDailyTotals(60);
        java.util.Map<LocalDate, Integer> byDay = new java.util.HashMap<>();
        for (DailyTotal t : totals)
            byDay.put(t.date(), t.totalMl());

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (byDay.getOrDefault(cursor, 0) >= goalMl) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    public record DailyTotal(LocalDate date, int totalMl) {
    }
}
